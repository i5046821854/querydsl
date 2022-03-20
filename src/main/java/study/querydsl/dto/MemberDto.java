package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MemberDto {

    public String username;
    public int age;

    @QueryProjection  //DTO도 Q파일을 생성하기 위해
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
