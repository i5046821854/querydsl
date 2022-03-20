package study.querydsl.entity;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

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

@Test
public void aggregation(){
        List<Tuple> fetch = qfactory.select(member.count(), member.age.sum(), member.age.avg(), member.age.max(), member.age.min()).from(member).fetch();  //데이터타입이 여러개일 경우 튜플을 씀. 하지만 실무에서는 그닥
        Tuple tuple = fetch.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
        }

@Test
public void group() throws Exception
        {
        List<Tuple> fetch = qfactory.select(team.name, member.age.avg())  //이때의 team, member는 qteam/ qmember임
        .from(member)
        .join(member.team, team)
        .groupBy(team.name).fetch();
        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
        }

@Test
public void join(){
        List<Member> result = qfactory.selectFrom(member)
        .leftJoin(member.team, team)  //rightjoin / 그냥  join도 가능
        .where(team.name.eq("teamA")).fetch();

        assertThat(result).extracting("username").containsExactly("member1", "member2");
        }

@Test
public void theta_join(){   //원래 세타 조인은 inner join 밖에 안됐는데, 최신 버전에서는 on을 사용해서  outer join도 가능
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        List<Member> result = qfactory.select(member)
        .from(member, team)
        .where(member.username.eq(team.name)).fetch();  //theta join => 일단 다 조인 한 다음에 where로 조건 주는 방식

        assertThat(result).extracting("username").containsExactly("teamA", "teamB");
        }

@Test  //on을 join의 필터로써 사용 / on으로 필터링하는 것은 outer join일때는 필수적이지만 inner join일 때는 필수적이지는 않음 (where에다가 조건 넣어주며 ㄴ됨)
public void join_on_filtering(){
        List<Tuple> teamA = qfactory.select(member, team).from(member)
        .leftJoin(member.team, team).on(team.name.eq("teamA")).fetch();

    for (Tuple tuple : teamA) {
        System.out.println("tuple = " + tuple);
    }
}

    @Test
    public void join_on_theta(){  //세타 조인처럼 연관관계가 없는 테이블 조인할 떄 on 을 사용용
       em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));
        List<Tuple> result = qfactory.select(member, team)
                .from(member)
                .leftJoin(team)  //leftJOin으로 쓸꺼면 엔티티 하나만 argument로 넣어주어야함 / 원래 그냥 join(inner join)같은 경우 (member.team ,team)이런식으로 한 반면..
                .on(member.username.eq(team.name)).fetch();  //관계 없는 두 테이블은 on 조건으로 필터링을 해줘야함
        for (Tuple tuple : result) {
            System.out.println("t="+ tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoin(){

        em.flush();
        em.clear();  //fetch join test는 영속성 지워주는 것이 국룰

        Member findMember = qfactory.selectFrom(member)
                .join(member.team, team).fetchJoin()    //join에다가 fetch join만 붙여주면 됨
                .where(member.username.eq("mebmer1")).fetchOne();
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());  //로딩이 되었는기
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void subQuery(){

        QMember memberSub = new QMember("memberSub");   //subquery는 알리아스를 다른걸 줘야하기 때문에

        List<Member> result = qfactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    @Test
    public void subQueryIn(){

        QMember memberSub = new QMember("memberSub");   //subquery는 알리아스를 다른걸 줘야하기 때문에
        List<Member> fetch = qfactory.selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)   //JPAExpressions를 이용해서 서브쿼리 표현 >> static import
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(fetch).extracting("age").containsExactly(20,30,40);
    }

    /**
     * 하지만 subquery는 from절에는 사용할 수 없음 >> 하기 위해서는 1. 서브쿼리를 join으로 바꾸거나 / 2. 어플리케이션에서 쿼리를 두번 분리해서 실행 / 3. nativeSQL을 사용
     */


    @Test
    public void subQuerySelect(){   //서브 쿼리가 select절 안에 있는 경우
        QMember memberSub = new QMember("memberSub");   //subquery는 알리아스를 다른걸 줘야하기 때문에

        List<Tuple> fetch = qfactory
                .select(member.username, JPAExpressions.select(memberSub).from(memberSub))
                .from(member).fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple=" + tuple);;
        }
    }

    /**
     * 하지만 백단에서 이렇게 case로 처리해주는 것은 바람직 하지 않을 수 있음. application layer로 일단 다 가지고 와서 처리해주는 것이 나음
     */
    @Test
    public void basicCase(){
        List<String> fetch = qfactory
                .select(member.age.when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member).fetch();
    }

    @Test
    public void basicComplexCase(){
        qfactory
                .select(
                        new CaseBuilder()
                                .when(member.age.between(0,20)).then("0~20살")
                                .otherwise("기타"))
                .from(member).fetch();
     }

     @Test
    public void constant(){  //튜플에 상수를 덧 붙이기 [member1, A] 형식으로
         List<Tuple> a = qfactory.select(member.username, Expressions.constant("A"))
                 .from(member).fetch();
         for (Tuple tuple : a) {
             System.out.println("tuple =" + tuple);
         }
     }

    @Test
    public void concat(){  //concatenation (다른 자료형 ) member1_100 형식으로
        List<String> a = qfactory.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member).where(member.username.eq("member1")).fetch();
        for (String string : a) {
            System.out.println("str =" + string);
        }
    }

    @Test
    public void tupleProjection(){ //자료형이 여러개 일 경우 튜플로 반환 / 하지만 튜플은 쿼리dsl에 종속적인 데이터 타입이므로 리포지토리 밖으로 보내지는 말것. 그
        List<Tuple> result = qfactory.select(member.username, member.age).from(member).fetch();
        for (Tuple tuple : result) {
            System.out.println("username " + tuple.get(member.username));
            System.out.println("age " + tuple.get(member.age));
        }
    }

    @Test  //setter로 DTO projection / 필드 이름이 파라미터 들이랑 같아야함 아니면 alias를 부여해야함
    public void findDtoBySetter(){
        List<MemberDto> fetch = qfactory
                .select(Projections.bean(MemberDto.class,
                        member.username.as("username"),  //이름이 다를때는 as로 맞춰줘야 함함
                        member.age)).from(member).fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto" + memberDto);
        }
    }

    @Test  //field로 DTO projection / 바로 필드에 꽂아버림 (setter말고) / 필드 이름이 파라미터 들이랑 같아야함 아니면 alias를 부여해야함
    public void findDtoByField(){
        List<MemberDto> fetch = qfactory.select(Projections.fields(MemberDto.class, member.username, member.age)).from(member).fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto" + memberDto);
        }
    }

    @Test  //생성자로 DTO projection / 생성자에 값 부여. 단 데이터 타입이 맞아야됨 (필드명 까지 맞춰줄 필요는 없음)
    public void findDtoByConstructor(){
        List<MemberDto> fetch = qfactory.select(Projections.constructor(MemberDto.class, member.username, member.age)).from(member).fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto" + memberDto);
        }
    }

    @Test  //subquery로 DTO projection
    public void findDtoBySetterWithSubquery(){
        QMember memberSub = new QMember("memberSub");
        List<MemberDto> fetch = qfactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        ExpressionUtils.as(JPAExpressions.select(memberSub.age.max()).from(memberSub),"age")    //서브쿼리로 뽑아올 때는 ExpressionUtils.as로 알리아스 맞춰줌
                )).from(member).fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto" + memberDto);
        }
    }

    @Test
    public void findDtoByQueryProjection(){  //DTO의 생성자 호출 방식으로 /Dto에 @queryProjection이 있어야함  //컴파일 타임에 에러를 잡을 수 있어서 좋음
        List<MemberDto> fetch = qfactory.select(new QMemberDto(member.username, member.age)).from(member).fetch();
        for (MemberDto memberDto : fetch) {
            System.out.println("memberDto" + memberDto);
        }
    }

    /**
     * 동적 쿼리 처리 using BooleanBuilder  / multiple wheres
     */

    @Test  //boolean builder 사용
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) { //Boolean Builder를 이용한 동적 쿼리 생성
        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameParam));  //초기값 부여 가능

//        if(usernameParam != null)
//        {
//            builder.and(member.username.eq(usernameParam));   //and / or 둘다 가능
//        }

        if(ageParam != null)
        {
            builder.or(member.age.eq(ageParam));
        }

        return qfactory.select(member).from(member).where(builder).fetch();
    }

    @Test  //다중 where문 사용  / 얘가 좋음
    public void dynamicQuery_WhereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }


    public List<Member> searchMember2(String usernameCond, Integer ageCond){
        return qfactory.selectFrom(member)
                //.where(usernameEq(usernameCond), ageEq(ageCond)) //두가지의 where 조건을 던지는데 null이 반환된다면 걍 무시됨
                .where(allEq(usernameCond, ageCond))  //한꺼번에 던짐
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;   //null이면 null반환
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond){ //여러 조건들을 조합해서 표현 가능
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    @Commit
    public void Bulkupdate(){
        long count = qfactory.update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();   //얘는 해당하는 모든 row를 바꾸긴 하지만 영속성 컨텍스트에 업데이트가 반영되진 않음

        em.flush();  //그래서 초기화 해줘야함
        em.clear();
    }

    @Test
    public void bulkAdd(){
        long count = qfactory.update(member)
                .set(member.age, member.age.add(1))  //각 row마다 age를 1씩 더함 / 빼기, 곱셈 등등 도 가능
                .execute();

        em.flush();
        em.clear();
    }

    @Test
    public void bulkDelete(){  //벌크로 삭제하기
        long count = qfactory.delete(member)
                .where(member.age.gt(18)).execute();

        em.flush();
        em.clear();
    }

    @Test
    public void functionCall(){
        String result = qfactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M")) //이 function은 각 DB dialect가 가지고 있는 함수여야함
                .from(member).fetchFirst();
    }

    @Test
    public void functionCall2(){
        String result = qfactory.select(member.username).from(member)
                .where(member.username.eq(Expressions.stringTemplate("function('lower',{0})", member.username))) //where 절에 function넣기
                .fetchFirst();
    }



}
