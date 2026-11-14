package cz.zcu.kiv.caretracker.dto.department;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportedHousingRequestDTO {
    private String name;
    private String street;
    private String city;
    private String postalCode;
}
