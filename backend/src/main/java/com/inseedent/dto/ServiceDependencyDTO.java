package com.inseedent.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceDependencyDTO {
    private String id;
    private String label;
    private String status;
    private List<String> dependencies;
}
