package org.example.datn_website_supershoes.service;

import org.example.datn_website_supershoes.Enum.Status;
import org.example.datn_website_supershoes.dto.request.ShoeSoleRequest;
import org.example.datn_website_supershoes.dto.response.ShoeSoleResponse;
import org.example.datn_website_supershoes.model.ShoeSole;
import org.example.datn_website_supershoes.repository.ShoeSoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShoeSoleService {

    @Autowired
    ShoeSoleRepository shoeSoleRepository;

    public List<ShoeSoleResponse> findAllShoeSole() {
        return shoeSoleRepository.findAllShoeSole();
    }

    public List<ShoeSoleResponse> findByStatus() {
        return shoeSoleRepository.findByStatus(Status.ACTIVE.toString());
    }

    public ShoeSole createShoeSole(ShoeSoleRequest shoeSoleRequest) {
        Optional<ShoeSole> shoeSole = shoeSoleRepository.findByName(shoeSoleRequest.getName());
        if (shoeSole.isPresent()) {
            throw new RuntimeException("Loại đế " + shoeSoleRequest.getName() + " đã tồn tại");
        }
        return shoeSoleRepository.save(convertShoeSoleRequestDTO(shoeSoleRequest));
    }

    public ShoeSole updateStatus(Long id, boolean aBoolean) {
        Optional<ShoeSole> shoeSole = shoeSoleRepository.findById(id);
        if (!shoeSole.isPresent()) {
            throw new RuntimeException("Không tìm thấy tài nguyên đế giày trong hệ thống!");
        }
        String newStatus = aBoolean ? Status.ACTIVE.toString() : Status.INACTIVE.toString();
        shoeSole.get().setStatus(newStatus);

        return shoeSoleRepository.save(shoeSole.get());


    }

    public ShoeSole convertShoeSoleRequestDTO(ShoeSoleRequest ShoeSoleRequest) {
        ShoeSole shoeSole = ShoeSole.builder()
                .name(ShoeSoleRequest.getName())
                .build();
        shoeSole.setStatus(Status.ACTIVE.toString());
        return shoeSole;
    }

}
