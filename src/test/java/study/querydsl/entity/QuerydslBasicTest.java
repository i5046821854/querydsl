package study.querydsl.entity;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory qfactory;

    @BeforeEach
    public void before() {
         qfactory= new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL(){
        String qlStr = "select m from Member m where m.username = :username";
        Member findMember = em.createQuery(qlStr, Member.class).setParameter("username", "member1").getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl(){
        Member findMember = qfactory.select(member).from(member).where(member.username.eq("member1")).fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDslwithAnd(){
        Member findMember = qfactory.selectFrom(member)  //select랑 from에 같은 것이 들어올 경우 붙여 쓸 수 있음
                                    .where(member.username.eq("member1"), member.age.lt(20) )   //여러 조건을 주고 싶을 때는 and / or 을쓰든지 and 같은 경우는 콤마로 연결해주면 됨
                                    .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void fetchTest(){
        List<Member> fetch = qfactory.selectFrom(member).fetch();  //전부 조회
        Member findMember = qfactory.selectFrom(member).fetchOne();  //하나만 조회
        Member findMember2 = qfactory.selectFrom(member).fetchFirst();  //처음의 한 건만 조회

        qfactory.selectFrom(member).fetchResults();  // 결과 반환해서 카운트 및 페이징 관련 메소드 제공해주느 ㄴ건데 deprecated

        long count =  qfactory.selectFrom(member).fetchCount();  //카운트 해주는 것이나 deprecated
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        List<Member> result = qfactory.selectFrom(member).where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())  //정렬 + 널 값 처리
                .fetch();
        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging() {
        List<Member> result = qfactory.selectFrom(member).orderBy(member.username.desc()).offset(1).limit(2).fetch();
        assertThat(result.size()).isEqualTo(2);
    }

}
