package com.sunt.NetworkTester.Service;

import com.sunt.NetworkTester.DTO.*;
import com.sunt.NetworkTester.Entity.DeviceEntity;
import com.sunt.NetworkTester.Entity.DeviceRuntimeStatus;
import com.sunt.NetworkTester.Exception.DeviceNotFoundException;
import com.sunt.NetworkTester.Repository.DeviceRepository;

import com.sunt.NetworkTester.Repository.DeviceRuntimeStatusRepository;
import com.sunt.NetworkTester.Service.Email.EmailSender;
import com.sunt.NetworkTester.mapper.DeviceMapper;
import jakarta.annotation.PreDestroy;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

//TODO: Optimizar procesos /**
//      Quitar la carpeta de windows defender (se come el 50% de los recursos por le ping)
//      Probar ping por tcp puerto 443 (averiguar si todos los dispositivos tienen abierta esta opcion)
// 
// TODO: MANEJO DE EXCEPCIONES POR DIOS!!

@Service
public class DeviceStatusService {

    private final DeviceRepository deviceRepository;
    private final DeviceRuntimeStatusRepository runtimeRepo;
    private final DeviceMapper mapper;

    private final SimpMessagingTemplate ws;

    private final Map<UUID, DeviceStatusDTO> prev = new ConcurrentHashMap<>();
    // cache de últimos estados

    private final ConcurrentHashMap<UUID, DeviceStatusDTO> cache = new ConcurrentHashMap<>();
    // control de “cuando fue el último ping” para cada device
    private final ConcurrentHashMap<UUID, Long> lastPingMillis = new ConcurrentHashMap<>();
    
    private final EmailSender emailSender;

    // Devuelve todos los dispositivos
    public List<DeviceEntity> findAll() {
        return deviceRepository.findAll();
    }
    
    // pool para pings concurrentes (ajusta a tu gusto)

    int cores = Runtime.getRuntime().availableProcessors();
    int core = Math.max(8, cores);      // base (8–16 suele ir bien)
    int max  = Math.max(16, cores * 2); // puede crecer si hay picos/offline
    long keepAlive = 30;                // hilos extra mueren tras 30 s ociosos

    BlockingQueue<Runnable> q = new LinkedBlockingQueue<>(2000); // cola grande pero acotada
    
    ThreadPoolExecutor pool = new ThreadPoolExecutor(
            core, max,
            keepAlive, TimeUnit.SECONDS,
            q,
            new ThreadPoolExecutor.CallerRunsPolicy() // backpressure: si se satura, el scheduler ejecuta la tarea y se “frena”
    );

    private static class OfflineInfo {
        long since;
        boolean alarmSent;
    }
    private final ConcurrentHashMap<UUID, OfflineInfo> offlineInfo = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Semaphore> deviceLocks = new ConcurrentHashMap<>();

    private boolean tryEnter(UUID id) {
        return deviceLocks.computeIfAbsent(id, k -> new Semaphore(1)).tryAcquire();
    }
    private void exit(UUID id) {
        var sem = deviceLocks.get(id);
        if (sem != null) sem.release();
    }
    
    int comprobacionSegundos = 0;

    public DeviceStatusService(DeviceRepository deviceRepository, DeviceRuntimeStatusRepository runtimeRepo, DeviceMapper mapper, SimpMessagingTemplate ws, EmailSender emailSender) {
        this.deviceRepository = deviceRepository;
        this.runtimeRepo = runtimeRepo;
        this.ws = ws;
        this.mapper = mapper;
        this.emailSender = emailSender;
    }
    
    //   ****|| CRUD ||****
    //Create
    public DeviceResponseDTO createDevice(DeviceCreateDTO dto) {
        DeviceEntity e = mapper.toEntity(dto);
        System.out.println(e.toString());
        e = deviceRepository.save(e);
        return mapper.toResponse(e);
        
        
    }
    //Delete
    public DeviceResponseDTO deleteDevice(String id) {
        
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
            
        }catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID no valido " + id);
        }
        
        if(!deviceRepository.existsById(uuid)) {
            //throw new DeviceNotFoundException("Dispositivo no encontrado con ID: " + id);;
        }

        DeviceResponseDTO dto = mapper.toResponse(deviceRepository.getById(uuid));
        deviceRepository.deleteById(uuid);
        return dto;
        
    }

    public DeviceResponseDTO updateDevice(String id, DeviceUpdateDTO dto) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("ID no válido: " + id);
        }

        DeviceEntity entity = deviceRepository.findById(uuid)
                .orElseThrow(() -> new DeviceNotFoundException("Dispositivo no encontrado con ID: " + id));

        // Actualizaciones (null-safe si quieres permitir parches parciales)
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getIp() != null) entity.setIp(dto.getIp());
        if (dto.getPingInterval() != null) entity.setPingInterval(dto.getPingInterval());
        if (dto.getTestAlways() != null) entity.setTestAlways(dto.getTestAlways());
        if (dto.getMinOfflineAlarm() != null) entity.setMinOfflineAlarm(dto.getMinOfflineAlarm());
        if (dto.getStartTime() != null) entity.setStartTime(dto.getStartTime());
        if (dto.getEndTime() != null) entity.setEndTime(dto.getEndTime());

        // Aquí está la conversión de List -> Set
        if (dto.getNotificationDays() != null) {
            entity.setNotifyDays(new LinkedHashSet<>(dto.getNotificationDays()));
            // Si no te importa el orden, puedes usar: new HashSet<>(...)
        }

        // Si aún no vas a gestionar las rules desde el update, no las toques:
        // entity.setRules(null);  // ← evita borrar reglas por accidente

        DeviceEntity saved = deviceRepository.save(entity);
        return mapper.toResponse(saved);
    }

    
    // ||               ||

    /** Devuelve estados en lote; si ids==null → todos */
    public List<DeviceStatusDTO> getStatuses(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>(cache.values());
        }
        return ids.stream()
                .map(cache::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void publishDelta(DeviceStatusDTO cur) {
        var last = prev.get(cur.id());
        if (last == null || last.online() != cur.online() || !Objects.equals(last.latencyMs(), cur.latencyMs())) {
            prev.put(cur.id(), cur);
            var msg = new DeviceRealtimeDTO(cur.id(), cur.online(), cur.latencyMs(), cur.updatedAt());
            ws.convertAndSend("/topic/devices/changes", msg); // ← PUSH
        }
    }

    private DeviceRuntimeStatus getOrCreateStatus(UUID id){
        return runtimeRepo.findById(id).orElseGet(() -> {
            DeviceRuntimeStatus s = new DeviceRuntimeStatus();
            s.setDeviceId(id);
            s.setLastState(null);
            s.setAlarmSent(false);
            return s;
        });
    }

    /** Tarea periódica: decide a quién pingear y lanza pings asíncronos */
    @Scheduled(fixedDelay = 1000)
    public void schedulerTick() {
        var devices = deviceRepository.findAll();
        long now = System.currentTimeMillis();

        for (DeviceEntity d : devices) {

            // Inicializa si no existe aún (jitter aleatorio)
            lastPingMillis.putIfAbsent(
                    d.getId(),
                    System.currentTimeMillis() - ThreadLocalRandom.current().nextLong(d.getPingInterval())
            );
            
            if (!debePingear(d, now)) continue;


            if (!pool.isShutdown()) {
                pool.submit(() -> {

                    if (!tryEnter(d.getId())) return; // ya hay uno con ese device
                    try {
                        lastPingMillis.put(d.getId(), now);
                        PingResult r = ping(d.getIp(), d.getPingInterval());
                        boolean isOnline = r.online();

                        // cache en memoria (para el front)
                        cache.put(d.getId(), new DeviceStatusDTO(d.getId(), isOnline, r.latencyMs(), Instant.now()));

                        var dto = new DeviceStatusDTO(d.getId(), isOnline, r.latencyMs(), Instant.now());
                        cache.put(d.getId(), dto);
                        publishDelta(dto);

                        // estado persistente
                        DeviceRuntimeStatus st = getOrCreateStatus(d.getId());

                        if (isOnline) {
                            st.setLastState(true);
                            st.setOfflineSince(null);
                            st.setAlarmSent(false);
                            runtimeRepo.save(st);
                            returnStatusConnection(new DeviceStatusDTO(d.getId(), true, r.latencyMs(), Instant.now()));
                        } else {
                            Instant nowTs = Instant.now();
                            if (st.getOfflineSince() == null) { // nueva caída
                                st.setOfflineSince(nowTs);
                                st.setAlarmSent(false);
                            }
                            st.setLastState(false);

                            long minutosOffline = java.time.Duration.between(st.getOfflineSince(), nowTs).toMinutes();
                            if (Boolean.FALSE.equals(st.getAlarmSent()) && minutosOffline >= d.getMinOfflineAlarm()) {
                                // ENVÍA una sola vez por caída (persistido)
                                System.out.println("\u001B[33m⚠️  [ALERTA] " + d.getName() + " (" + d.getIp() +
                                        ") ha superado " + d.getMinOfflineAlarm() + " min offline\u001B[0m");
                                //sendAlarmEmail(d);
                                onOfflineThreshold(d);
                                st.setAlarmSent(true);
                                st.setLastAlarmAt(nowTs);
                            }
                            runtimeRepo.save(st);
                        }

                        // logs de color (opcional)
                        if (isOnline) {
                            //System.out.println("\u001B[32m[PING OK] " + d.getName() + " (" + d.getIp() + ")\u001B[0m");
                        } else {
                            //System.out.println("\u001B[31m[PING FAIL] " + d.getName() + " (" + d.getIp() + ")\u001B[0m");
                        }
                    } finally {
                        exit(d.getId());
                    }
                    
                    
                });            
            }

            
        }
    }


    /** Lógica para decidir si hay que pingear ahora */
    private boolean debePingear(DeviceEntity d, long nowMillis) {
        // 1) Respeta siempre la frecuencia (freno mínimo)
        long interval = Math.max(500L, d.getPingInterval()); // seguridad mínimo 500ms
        long last = lastPingMillis.getOrDefault(d.getId(), 0L);
        if ((nowMillis - last) < interval) {
            //System.out.println(comprobacionSegundos + 1 + " segundos pasados");
            return false;
        }

        // 2) Si está en modo testAlways → ignora día/horario, pero mantiene la frecuencia
        if (d.isTestAlways()) return true;

        // 3) Evalúa día de la semana y horario (zona de Madrid)
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        DayOfWeek today = now.getDayOfWeek();
        LocalTime time = now.toLocalTime();

        if (!isAllowedDay(d, today)){
            //System.out.println("Hoy es miercoles no toca pingear");
            return false;  
        } 
        if (!isWithinSchedule(d, time)){

            return false;
        }
        //System.out.println("Hoy es miercoles toca pingear");
        return true;
    }

    private boolean isAllowedDay(DeviceEntity d, DayOfWeek today) {
        Set<DayOfWeek> days = d.getNotifyDays();
        if (days == null || days.isEmpty()) {
            // Sin restricción → se permite cualquier día
            return true;
        }
        return days.contains(today);
    }

    /** Devuelve true si la hora actual está dentro del horario permitido.
     *  Soporta ventana normal (start <= end) y ventana que cruza medianoche (start > end).
     *  Si no hay horario configurado, asume "24/7".
     */
    private boolean isWithinSchedule(DeviceEntity d, LocalTime now) {
        // Ajusta los getters si usas otros nombres o estructuras
        LocalTime start = d.getStartTime();
        LocalTime end   = d.getEndTime();

        if (start == null || end == null) {
            // sin horario → 24/7
            return true;
        }
        return isWithinWindow(now, start, end);
    }

    /** Comprueba una ventana horaria, incluyendo las que cruzan medianoche. */
    private boolean isWithinWindow(LocalTime now, LocalTime start, LocalTime end) {
        if (start.equals(end)) {
            // misma hora = interpretamos como 24h (si quisieras "cerrado", cámbialo a false)
            return true;
        }
        if (start.isBefore(end)) {
            // ventana normal: [start, end]
            return !now.isBefore(start) && !now.isAfter(end);
        } else {
            // ventana cruza medianoche, p.ej. 22:00–06:00 → [start, 24:00) U [00:00, end]
            return !now.isBefore(start) || !now.isAfter(end);
        }
    }

    /** Resultado de ping */
    private record PingResult(boolean online, Long latencyMs) {}

    /** Ping por SO usando comando nativo (fiable cuando ICMP está permitido) */
    private PingResult ping(String ip, int timeoutMs) {
        String os = System.getProperty("os.name").toLowerCase();
        List<String> cmd;
        if (os.contains("win")) {
            // Windows: -n 1 (una trama), -w timeout(ms)
            cmd = List.of("cmd.exe", "/c", "ping", "-n", "1", "-w", String.valueOf(timeoutMs), ip);
        } else {
            // Linux/macOS: -c 1 (una trama), -W timeout(s)
            int sec = Math.max(1, timeoutMs / 1000);
            cmd = List.of("sh", "-c", "ping -c 1 -W " + sec + " " + ip);
        }

        long start = System.nanoTime();
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();

            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.append(line).append('\n');
                }
            }

            // Espera con margen y toma exit code
            boolean finished = p.waitFor(timeoutMs + 200L, TimeUnit.MILLISECONDS);
            int exit = finished ? p.exitValue() : 999;

            String stdout = out.toString().toLowerCase();

            // Soporta ping en ES y EN
            boolean containsOk =
                    stdout.contains("ttl=") ||
                            stdout.contains("time=") ||      // EN
                            stdout.contains("tiempo=");      // ES

            boolean success = (exit == 0) || containsOk;

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

            // Log ÚNICO por ping

            return success ? new PingResult(true, elapsedMs) : new PingResult(false, null);
        } catch (Exception e) {
            System.out.println("\u001B[31m[PING] ip=" + ip + " success=false EXCEPTION=" + e.getClass().getSimpleName() + "\u001B[0m");
            return new PingResult(false, null);
        }
    }
    
    private void sendAlarmEmail(DeviceEntity d) {
        System.out.println("\u001B[33m⚠️  [Enviando Email...] \u001B[0m");
        String username = "developadri@gmail.com";      // tu dirección de Gmail
        String appPassword = "gcyx vgvf ruws fgnt"; // ponla en variable de entorno

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // para 587
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        // Si prefieres 465 (SSL puro), usa:
        // props.put("mail.smtp.ssl.enable", "true");
        // props.put("mail.smtp.port", "465");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, appPassword);
            }
        });

        Message msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(username, "Argos NetworkTester"));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        try {
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse("developadri@gmail.com"));
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        try {
            msg.setSubject("⚠️  [ALERTA] El dispositivo: " + d.getName() + " con ip: " + d.getIp() + " lleva desconectado " + d.getMinOfflineAlarm() + " minutos.");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        try {
            msg.setText("⚠️  [ALERTA] El dispositivo: " + d.getName() + " con ip: " + d.getIp() + " lleva desconectado " + d.getMinOfflineAlarm() + " minutos.");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

        try {
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Enviado ✅");

    }

    private void onOfflineThreshold(DeviceEntity d) {
        emailSender.sendDeviceOfflineAlert(d);
    }
    
    private ResDeviceConnTest returnStatusConnection (DeviceStatusDTO deviceStatusDTO){
        
        ResDeviceConnTest rCT = new ResDeviceConnTest(
                deviceStatusDTO.id(),
                deviceStatusDTO.online()
                
        );
        return rCT;
    }

    /**
     * Este método se ejecuta automáticamente cuando el contenedor de Spring
     * (por ejemplo, al parar IntelliJ o hacer Stop en el Run) destruye el bean.
     */
    @PreDestroy
    private void onShutdown() {
        System.out.println("[SHUTDOWN] Cerrando thread pool de pings...");
        pool.shutdown(); // deja terminar las tareas activas

        try {
            if (!pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                System.out.println("[SHUTDOWN] Forzando cierre inmediato del pool...");
                pool.shutdownNow(); // corta tareas pendientes
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("[SHUTDOWN] Pool cerrado correctamente.");
    }

}
