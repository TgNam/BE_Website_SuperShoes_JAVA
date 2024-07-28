package org.example.datn_website_supershoes.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "Material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Material extends BaseEntity {

    @Column
    private String name;

    @JsonManagedReference(value = "materialProductReference")
    @OneToMany(mappedBy = "material")
    private List<Product> products;

}
