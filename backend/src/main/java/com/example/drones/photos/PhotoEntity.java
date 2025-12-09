package com.example.drones.photos;

import com.example.drones.operators.PortfolioEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "photos")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PhotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "portfolio_id", insertable = false, updatable = false)
    private Integer portfolioId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "url", nullable = false)
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private PortfolioEntity portfolio;

}
