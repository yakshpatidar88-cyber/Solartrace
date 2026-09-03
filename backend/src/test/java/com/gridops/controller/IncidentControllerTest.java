package com.gridops.controller;

import com.gridops.domain.MaintenanceIncident;
import com.gridops.dto.OperationalDtos.IncidentResponseDto;
import com.gridops.repository.MaintenanceIncidentRepository;
import com.gridops.service.MaintenanceIncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IncidentController.class)
class IncidentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaintenanceIncidentService incidentService;

    @MockBean
    private MaintenanceIncidentRepository incidentRepository;

    @Test
    void testGetAllActiveIncidents_ReturnsList() throws Exception {
        UUID incidentId = UUID.randomUUID();
        IncidentResponseDto dto = IncidentResponseDto.builder()
                .id(incidentId)
                .title("[P1_CRITICAL] Inverter Trip on INV-01")
                .priority(MaintenanceIncident.IncidentPriority.P1_CRITICAL)
                .priorityScore(BigDecimal.valueOf(88.50))
                .status(MaintenanceIncident.IncidentStatus.OPEN)
                .assetName("Central Inverter INV-01")
                .siteName("Mojave Solar Complex")
                .build();

        when(incidentService.getAllActiveIncidents()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("[P1_CRITICAL] Inverter Trip on INV-01"))
                .andExpect(jsonPath("$[0].priority").value("P1_CRITICAL"));
    }
}
