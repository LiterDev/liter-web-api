package io.liter.web.api.follower;

import io.liter.web.api.follower.view.FollowerList;
import io.liter.web.api.follower.view.FollowerPost;
import io.liter.web.api.review.view.Pagination;
import io.liter.web.api.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.springframework.web.reactive.function.server.ServerResponse.*;

@Slf4j
@Component
public class FollowerHandler {

    //todo::getAllMyFollower        ->  findAll
    //todo::getAllUserIdFollower    ->  findAllByUserId
    //todo::post                    ->  post
    //todo::put                     ->  put

    private final ReactiveMongoTemplate mongoTemplate;

    private final UserRepository userRepository;

    private final FollowerRepository followerRepository;

    public FollowerHandler(
            ReactiveMongoTemplate mongoTemplate
            , UserRepository userRepository
            , FollowerRepository followerRepository
    ) {
        this.mongoTemplate = mongoTemplate;
        this.userRepository = userRepository;
        this.followerRepository = followerRepository;
    }

    /**
     * test001 -> test002, test003
     */

    /**
     * GET All Follower by MyId
     */
    public Mono<ServerResponse> findAll(ServerRequest request) {
        log.info("]-----] FollowerHandler::findAll call [-----[ ");

        /**
         * 나를 팔로워 하는 사람들
         */

        FollowerList followerList = new FollowerList();
        Pagination pagination = new Pagination();

        Integer page = request.queryParam("page").isPresent() ? Integer.parseInt(request.queryParam("page").get()) : 0;
        Integer size = request.queryParam("size").isPresent() ? Integer.parseInt(request.queryParam("size").get()) : 10;

        log.info("]-----] page [-----[ {}", page);
        log.info("]-----] size [-----[ {}", size);


        return request.principal()
                .flatMap(p -> this.userRepository.findByUsername(p.getName()))
                .flatMap(user -> this.followerRepository.findByUserId(user.getId()))
                .flatMap(follower -> this.userRepository.findAllById(follower.getFollowerId())
                        .collectList()
                        .map(collections -> {
                            //todo::팔로워가 나를 팔로잉하고 있는지 확인

                            followerList.setFollowerUser(collections);
                            return follower;
                        }))
                .flatMap(follower -> this.userRepository.countByIdIn(follower.getFollowerId()))
                .flatMap(count -> {
                    pagination.setTotal(count);
                    pagination.setPage(page);
                    pagination.setSize(size);

                    followerList.setPagination(pagination);

                    return ok().body(BodyInserters.fromObject(followerList));
                })
                .switchIfEmpty(notFound().build());
    }

    /**
     * GET All Follower by UserId
     */
    public Mono<ServerResponse> findAllByUserId(ServerRequest request) {
        log.info("]-----] FollowerHandler::findAllByUserId call [-----[ ");

        /**
         * 특정 유저를 팔로워 하는 사람들
         */

        FollowerList followerList = new FollowerList();
        Pagination pagination = new Pagination();

        Integer page = request.queryParam("page").isPresent() ? Integer.parseInt(request.queryParam("page").get()) : 0;
        Integer size = request.queryParam("size").isPresent() ? Integer.parseInt(request.queryParam("size").get()) : 10;

        ObjectId userId = new ObjectId(request.pathVariable("userId"));

        return request.principal()
                .flatMap(p -> this.userRepository.findByUsername(p.getName()))
                .flatMap(user -> this.followerRepository.findByUserId(userId))
                .flatMap(follower -> this.userRepository.findAllById(follower.getFollowerId())
                        .collectList()
                        .map(collections -> {
                            //todo::팔로워가 나를 팔로잉하고 있는지 확인

                            followerList.setFollowerUser(collections);
                            return follower;
                        }))
                .flatMap(follower -> this.userRepository.countByIdIn(follower.getFollowerId()))

                .flatMap(count -> {
                    pagination.setTotal(count);
                    pagination.setPage(page);
                    pagination.setSize(size);

                    followerList.setPagination(pagination);

                    return ok().body(BodyInserters.fromObject(followerList));
                })
                .switchIfEmpty(notFound().build());
    }

    /**
     * POST a Follower
     */


    public Mono<ServerResponse> post(ServerRequest request) {
        log.info("]-----] FollowerHandler::post call [-----[ ");
        /**
         * 1. jwt 나의 user 정보
         * 2. 팔로워 하려는(userId) 유저를 내가 이미 팔로워 하고 있는지 확인
         * 3. boolean == false 이면, Set.add(내 아이디)
         */
        Query query = new Query();
        Update update = new Update();

        Query query2 = new Query();
        Update update2 = new Update();

        ObjectId userId = new ObjectId(request.pathVariable("userId"));

        return request.principal()
                .flatMap(p -> this.userRepository.findByUsername(p.getName())
                        .filter(user -> Objects.equals(userId, user.getId()) == false))

                .doOnNext(user -> query.addCriteria(Criteria.where("userId").is(userId)))
                .doOnNext(user -> update.currentTimestamp("updateAt"))

                .doOnNext(user -> query2.addCriteria(Criteria.where("userId").is(userId))
                        .addCriteria(Criteria.where("followerId").ne(user.getId())))
                .doOnNext(user -> update2.addToSet("followerId", user.getId())
                        .inc("followerCount", 1)
                        .currentTimestamp("updateAt"))

                .flatMap(user -> mongoTemplate.upsert(query, update, Follower.class))
                .flatMap(result -> mongoTemplate.findAndModify(query2, update2, FindAndModifyOptions.options().returnNew(true).upsert(false), Follower.class))
                .flatMap(follower -> ok().body(BodyInserters.fromObject(follower)))
                .switchIfEmpty(notFound().build());
    }

    /**
     * PUT a Follower
     */
    public Mono<ServerResponse> put(ObjectId followerId, ObjectId userId) {
        log.info("]-----] FollowerHandler::insertFollower call [-----[ ");

        Follower follower = new Follower();
        Set<ObjectId> idSet = new HashSet<>();

        idSet.add(followerId);

        follower.setUserId(userId);
        follower.setFollowerCount(idSet.size());
        follower.setFollowerId(idSet);

        return ok().body(this.followerRepository.save(follower), Follower.class)
                .switchIfEmpty(badRequest().build());
    }
}