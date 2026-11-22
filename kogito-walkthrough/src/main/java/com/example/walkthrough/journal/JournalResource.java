package com.example.walkthrough.journal;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Endpoint buatan sendiri (bukan hasil generate Kogito) untuk melihat isi {@link ProcessJournal}. */
@Path("/journal")
@Produces(MediaType.APPLICATION_JSON)
public class JournalResource {

    @Inject
    ProcessJournal journal;

    @GET
    @Path("/{processInstanceId}")
    public ProcessJournal.Entry get(@PathParam("processInstanceId") String processInstanceId) {
        return journal.find(processInstanceId)
                .orElseThrow(() -> new NotFoundException("process instance " + processInstanceId + " belum tercatat"));
    }
}
