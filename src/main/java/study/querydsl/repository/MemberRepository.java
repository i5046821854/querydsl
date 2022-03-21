package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

/**
 * spring data JPA를 이용하여 리포지토리 생성
 */
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {   //jpaRepository와 커스텀한 MemberRepositoryCustom을 모두 상속 받음

    List<Member> findByUsername(String username);
}
