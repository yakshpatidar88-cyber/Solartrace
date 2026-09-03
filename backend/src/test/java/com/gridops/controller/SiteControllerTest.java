package com.gridops.controller;

import com.gridops.domain.Site;
import com.gridops.repository.SiteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SiteController.class)
class SiteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SiteRepository siteRepository;

    @Test
    void testGetAllSites_ReturnsSuccess() throws Exception {
        UUID siteId = UUID.randomUUID();
        Site site = Site.builder()
                .id(siteId)
                .name("Desert Sunlight Solar Farm")
                .location("California, USA")
                .capacityMw(BigDecimal.valueOf(50.0))
                .gridConnectionType("HVDC_INTERCONNECT")
                .build();

        when(siteRepository.findAll()).thenReturn(List.of(site));

        mockMvc.perform(get("/api/v1/sites").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Desert Sunlight Solar Farm"))
                .andExpect(jsonPath("$[0].capacityMw").value(50.0));
    }

    @Test
    void testGetSiteById_NotFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(siteRepository.findById(missingId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/sites/" + missingId).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
