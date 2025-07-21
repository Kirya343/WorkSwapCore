package org.workswap.core.datasource.central.model.listingModels;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
public class Location {

    public Location(String name,
                    boolean city,
                    Location country) {
        this.name = name;
        this.city = city;
        this.country = country;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    private boolean city = false;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Location country;

    @OneToMany(mappedBy = "country", cascade = CascadeType.ALL)
    private List<Location> cities = new ArrayList<>();

    @Transient
    private String fullName;

    public String getFullName() {
        String locationName;
        if (!isCity()) {
            locationName = name.toString();
        } else {
            locationName = country.getName() + ", " + name;
        }
        return locationName;
    }
}
