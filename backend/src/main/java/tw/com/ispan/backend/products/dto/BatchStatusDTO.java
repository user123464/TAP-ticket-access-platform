package tw.com.ispan.backend.products.dto;

import java.util.List;

import lombok.Data;

@Data
public class BatchStatusDTO {
    private List<Integer> productId;
    private String organizerId;
    private String status;
}
