package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

public class MemberRepositoryImpl implements  MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    public MemberRepositoryImpl(EntityManager em)
    {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition condition) //where params로 동적쿼리 생성
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

    @Override   //단순 페이징 처리
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory.select(new QMemberTeamDto(
                member.id.as("memberID"), member.username, member.age, team.id.as("teamId"), team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))  //동적쿼리
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();
        List<MemberTeamDto> results1 = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(results1, pageable, total);

    }

    @Override  //페이징인데, 컨텐트랑 카운트를 따로 / 카운트 쿼리를 먼저하고 컨텐츠를 가져오는 경우 , left join을 카운트 쿼리에는 나타내기 싫은 경우 쓰면 좋음
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {
        List<MemberTeamDto> results = queryFactory.select(new QMemberTeamDto(
                member.id.as("memberID"), member.username, member.age, team.id.as("teamId"), team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()))  //동적쿼리
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

//        long total = queryFactory.select(member).from(member).leftJoin(member.team, team)
        JPAQuery<Member> countQuery = queryFactory.select(member).from(member).leftJoin(member.team, team)   //카운트 쿼리의 최적화를 위해
                .where(usernameEq(condition.getUsername()),
                        teamNameEq(condition.getTeamName()),
                        ageGoe(condition.getAgeGoe()),
                        ageLoe(condition.getAgeLoe()));
        //.fetchCount();

        return PageableExecutionUtils.getPage(results, pageable, ()->countQuery.fetchCount());  //카운트 쿼리의 최적화 (마지막 페이지나 한 페이지에 다 들어가는 경우는 카운터 쿼리 안들어감)
//        return new PageImpl<>(results, pageable, total);

    }
}
