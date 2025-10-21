package com.sunt.NetworkTester.Service;

import com.sunt.NetworkTester.DTO.DeviceRealtimeDTO;
import com.sunt.NetworkTester.DTO.DeviceStatusDTO;
import com.sunt.NetworkTester.DTO.ResDeviceConnTest;
import com.sunt.NetworkTester.Entity.DeviceEntity;
import com.sunt.NetworkTester.Entity.DeviceRuntimeStatus;
import com.sunt.NetworkTester.Repository.DeviceRepository;

import com.sunt.NetworkTester.Repository.DeviceRuntimeStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeviceStatusService {

    private final DeviceRepository deviceRepository;
    private final DeviceRuntimeStatusRepository runtimeRepo;

    private final SimpMessagingTemplate ws;

    private final Map<UUID, DeviceStatusDTO> prev = new ConcurrentHashMap<>();
    // cache de últimos estados
    private final ConcurrentHashMap<UUID, DeviceStatusDTO> cache = new ConcurrentHashMap<>();
    // control de “cuando fue el último ping” para cada device
    private final ConcurrentHashMap<UUID, Long> lastPingMillis = new ConcurrentHashMap<>();

    // Devuelve todos los dispositivos
    public List<DeviceEntity> findAll() {
        return deviceRepository.findAll();
    }
    
    // pool para pings concurrentes (ajusta a tu gusto)
    
    private final ExecutorService pool = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 2));

    private static class OfflineInfo {
        long since;
        boolean alarmSent;
    }
    private final ConcurrentHashMap<UUID, OfflineInfo> offlineInfo = new ConcurrentHashMap<>();

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
            if (!debePingear(d, now)) continue;
            lastPingMillis.put(d.getId(), now);

            pool.submit(() -> {
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
                        sendAlarmEmail(d);
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
            });
        }
    }


    /** Lógica simple para decidir si hay que pingear ahora */
    private boolean debePingear(DeviceEntity d, long nowMillis) {
        // Si está marcado “testAlways”, ignoramos schedule
        if (!d.isTestAlways()) {
            // muy simple: solo ejemplo. Aquí puedes evaluar d.getSchedule() (Antes de N1/N2…)
            // Si no cumple ventana, devolver false
        }
        long interval = Math.max(500L, d.getPingInterval()); // seguridad mínimo 500ms
        long last = lastPingMillis.getOrDefault(d.getId(), 0L);
        return (nowMillis - last) >= interval;
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
        System.out.println("\u001B[33m⚠️  [ALERTA] El dispositivo " + d.getName() +
                " (" + d.getIp() + ") ha superado el tiempo de alarma (" + d.getMinOfflineAlarm() + " min)\u001B[0m");

    }
    
    private ResDeviceConnTest returnStatusConnection (DeviceStatusDTO deviceStatusDTO){
        
        ResDeviceConnTest rCT = new ResDeviceConnTest(
                deviceStatusDTO.id(),
                deviceStatusDTO.online()
                
        );
        return rCT;
    }

}
