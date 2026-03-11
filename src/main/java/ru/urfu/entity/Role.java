package ru.urfu.entity;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

    public String getNameWithoutRole(){ //userPermission

        String[] roles = name.split("_");
        String roles1 = roles[1];
        String[] roles2 = roles1.split("]");
        return roles2[0];


//        String[] roles = name.split("_");
//        return roles[1];
    }


}
