package ubc.pavlab.rdp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import ubc.pavlab.rdp.model.enums.TierType;

import javax.persistence.*;

/**
 * Created by mjacobson on 17/01/18.
 */
@Entity
@Table(name = "gene")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = {"gene"})
@ToString
public class UserGene {

    @JsonIgnore
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private int id;

    @Enumerated(EnumType.STRING)
    @Column
    private TierType tier;

    @Embedded
    @JsonUnwrapped
    private Gene gene;

}