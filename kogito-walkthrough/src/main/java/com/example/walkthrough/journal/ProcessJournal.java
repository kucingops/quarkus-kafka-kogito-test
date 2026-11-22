package com.example.walkthrough.journal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.kie.api.event.process.ProcessCompletedEvent;
import org.kie.api.event.process.ProcessNodeTriggeredEvent;
import org.kie.kogito.internal.process.event.DefaultKogitoProcessEventListener;
import org.kie.kogito.internal.process.runtime.KogitoProcessInstance;
import org.kie.kogito.internal.process.runtime.KogitoWorkflowProcessInstance;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Process event listener: Kogito memanggil bean ini setiap kali sesuatu terjadi di proses
 * mana pun. Di sini dipakai untuk mencatat node yang dilewati dan variabel akhir proses,
 * karena proses yang sudah selesai langsung dihapus dari penyimpanan in-memory Kogito
 * (GET /{processId}/{id} akan mengembalikan 404).
 *
 * Hasilnya bisa dilihat di {@code GET /journal/{processInstanceId}}.
 */
@ApplicationScoped
public class ProcessJournal extends DefaultKogitoProcessEventListener {

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void beforeNodeTriggered(ProcessNodeTriggeredEvent event) {
        String name = event.getNodeInstance().getNodeName();
        if (name != null && !name.isBlank()) {
            entry(event.getProcessInstance().getId(), event.getProcessInstance().getProcessId()).addNode(name);
        }
    }

    @Override
    public void afterProcessCompleted(ProcessCompletedEvent event) {
        KogitoProcessInstance pi = (KogitoProcessInstance) event.getProcessInstance();
        Entry entry = entry(pi.getStringId(), pi.getProcessId());
        if (pi instanceof KogitoWorkflowProcessInstance wpi) {
            entry.complete(wpi.getVariables());
        } else {
            entry.complete(Map.of());
        }
    }

    public Optional<Entry> find(String processInstanceId) {
        return Optional.ofNullable(entries.get(processInstanceId));
    }

    private Entry entry(String processInstanceId, String processId) {
        return entries.computeIfAbsent(processInstanceId, id -> new Entry(id, processId));
    }

    /** Catatan satu process instance. Bentuk JSON-nya yang dikembalikan {@code /journal}. */
    public static class Entry {
        private final String processInstanceId;
        private final String processId;
        private final List<String> nodes = new ArrayList<>();
        private volatile boolean completed;
        private volatile Instant completedAt;
        private volatile Map<String, Object> variables = Map.of();

        Entry(String processInstanceId, String processId) {
            this.processInstanceId = processInstanceId;
            this.processId = processId;
        }

        synchronized void addNode(String node) {
            nodes.add(node);
        }

        void complete(Map<String, Object> vars) {
            this.variables = new HashMap<>(vars);
            this.completedAt = Instant.now();
            this.completed = true;
        }

        public String getProcessInstanceId() {
            return processInstanceId;
        }

        public String getProcessId() {
            return processId;
        }

        public synchronized List<String> getNodes() {
            return List.copyOf(nodes);
        }

        public boolean isCompleted() {
            return completed;
        }

        public Instant getCompletedAt() {
            return completedAt;
        }

        public Map<String, Object> getVariables() {
            return variables;
        }
    }
}
