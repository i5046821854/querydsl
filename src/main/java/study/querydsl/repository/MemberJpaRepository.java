package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;


    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }

/*
    //main에 jpaQueryFactory를 Bean으로 지정해준다면
    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
        this.em = em;
        this.queryFactory = queryFactory;
    }
*/


    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll(){
        return em.createQuery("select m from Member m", Member.class).getResultList();
    }

    public List<Member> findAll_Querydsl(){  //queryDsl로 표현한 findAll
        return queryFactory.selectFrom(member).fetch();
    }


    public List<Member>  findByUsername(String username){
        List<Member> username1 = em.createQuery("select m from Member m where m.username = :username", Member.class).setParameter("username", username).getResultList();
        return username1;
    }

    public List<Member> findByUsername_Querydsl(String username){  //queryDsl로 표현한 findAll
        return queryFactory.selectFrom(member)
                .where(member.username.eq(username)).fetch();
    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition)  //builder로 동적쿼리 생성
    {
        BooleanBuilder builder = new BooleanBuilder();
        if (StringUtils.hasText(condition.getUsername())) {  //null 말고도 ""가 오는 경우도 걸러넴
            builder.and(member.username.eq(condition.getUsername()));
        }
        if (StringUtils.hasText(condition.getTeamName())) {  //null 말고도 ""가 오는 경우도 걸러넴
            builder.and(team.name.eq(condition.getTeamName()));
        }
        if (condition.getAgeGoe()!= null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if (condition.getAgeLoe()!= null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory.select(new QMemberTeamDto(
                member.id.as("memberID"), member.username, member.age, team.id.as("teamId"), team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(builder)  //동적쿼리
                .fetch();
    }

    public List<MemberTeamDto> searchByWhereParam(MemberSearchCondition condition) //where params로 동적쿼리 생성
    {
        return queryFactory.select(new QMemberTeamDto(
                member.id.as("memberID"), member.username, member.age, team.id.as("teamId"), team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))  //동적쿼리
                .fetch();
    }

    public List<MemberTeamDto> searchMemberByWhereParam(MemberSearchCondition condition) //프로젝션 사항이 바뀌어도 그냥 selectFrom의 argument만 바꿔주면 됨
    {
        return queryFactory.select(new QMemberTeamDto(
            member.id.as("memberID"), member.username, member.age, team.id.as("teamId"), team.name.as("teamName")))
            .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))  //이 모두를 조립할 수도 있음
                .fetch();
    }


    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe != null ? member.age.loe(ageLoe) : null;
    }

    private Predicate ageGoe(Integer ageGoe) {
        return ageGoe != null ? member.age.goe(ageGoe) : null;
    }

    private Predicate teamNameEq(String teamName) {
        return teamName != null ? team.name.eq(teamName) : null;
    }

    private Predicate usernameEq(String username) {
        return username != null ? member.username.eq(username) : null;
    }
}
