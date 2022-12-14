package com.example.sns.service;

import com.example.sns.exception.ErrorCode;
import com.example.sns.exception.SnsApplicationException;
import com.example.sns.model.AlarmArgs;
import com.example.sns.model.AlarmType;
import com.example.sns.model.Comment;
import com.example.sns.model.Post;
import com.example.sns.model.entity.*;
import com.example.sns.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostEntityRepository postEntityRepository;
    private final UserEntityRepository userEntityRepository;
    private final LikeEntityRepository likeEntityRepository;
    private final CommentEntityRepository commentEntityRepository;
    private final AlarmEntityRepository alarmEntityRepository;

    @Transactional
    public void create(String title, String body, String userName) {
        UserEntity userEntity = getUserOrException(userName);

        // post save
        postEntityRepository.save(PostEntity.of(title, body, userEntity));
    }

    @Transactional
    public Post modify(String title, String body, String userName, Integer postId) {
        UserEntity userEntity = getUserOrException(userName);
        PostEntity postEntity = getPostOrException(postId);

        //post permission
        if(postEntity.getUser() != userEntity) {
            throw new SnsApplicationException(ErrorCode.INVALID_PERMISSION, String.format("%s has no permission with %s", userName,postId));
        }
        postEntity.setTitle(title);
        postEntity.setBody(body);

        return Post.fromEntity(postEntityRepository.saveAndFlush(postEntity));
    }

    @Transactional
    public void delete(String userName, Integer postId) {
        UserEntity userEntity = getUserOrException(userName);
        PostEntity postEntity = getPostOrException(postId);
        //permission
        if(postEntity.getUser() != userEntity) {
            throw new SnsApplicationException(ErrorCode.INVALID_PERMISSION, String.format("%s has no permission with %s", userName,postId));
        }

        postEntityRepository.delete(postEntity);
    }

    public Page<Post> list(Pageable pageable) {
        return postEntityRepository.findAll(pageable).map(Post::fromEntity);
    }

    public Page<Post> myPosts(String userName, Pageable pageable) {
        UserEntity userEntity = getUserOrException(userName);

        return postEntityRepository.findAllByUser(userEntity, pageable).map(Post::fromEntity);
    }
    @Transactional
    public void like(Integer postId, String userName) {
        PostEntity postEntity = getPostOrException(postId);
        UserEntity userEntity = getUserOrException(userName);
        // "?????????"??? ????????? ?????? ??? ????????? ??????.
        // check like => throw
        likeEntityRepository.findByUserAndPost(userEntity, postEntity).ifPresent(it -> {
                throw new SnsApplicationException(ErrorCode.ALREADY_LIKE, String.format("%s is already liked the post %s", userName, postId));
        });

        likeEntityRepository.save(LikeEntity.of(userEntity,postEntity));

        //alram save
        alarmEntityRepository.save(AlarmEntity.of(postEntity.getUser(), AlarmType.NEW_LIKE_ON_POST, new AlarmArgs(userEntity.getId(),postEntity.getId())));
    }

    @Transactional
    public int likeCount(Integer postId) {
        PostEntity postEntity = getPostOrException(postId);

        // count like
/*      List<LikeEntity> likeEntitys = likeEntityRepository.findAllByPost(postEntity);
        return likeEntitys.size();
*/
        return likeEntityRepository.countByPost(postEntity);
    }

    @Transactional
    public void comment(Integer postId, String userName, String comment) {
        PostEntity postEntity = getPostOrException(postId);
        UserEntity userEntity = getUserOrException(userName);

        //comment save
        commentEntityRepository.save(CommentEntity.of(userEntity,postEntity,comment));

        //alram save
        alarmEntityRepository.save(AlarmEntity.of(postEntity.getUser(), AlarmType.NEW_COMMENT_ON_POST, new AlarmArgs(userEntity.getId(),postEntity.getId())));
    }

    @Transactional
    public Page<Comment> getComment(Integer postId, Pageable pageable) {
        PostEntity postEntity = getPostOrException(postId);
        //comment save
       return commentEntityRepository.findAllByPost(postEntity, pageable).map(Comment::fromEntity);
    }

    // post exist
    private PostEntity getPostOrException(Integer postId) {
        return postEntityRepository.findById(postId).orElseThrow(() ->
                new SnsApplicationException(ErrorCode.POST_NOT_FOUNDED, String.format("%s not founded", postId)));
    }

    // user exist
    private UserEntity getUserOrException(String userName) {
        return userEntityRepository.findByUserName(userName).orElseThrow(()
                -> new SnsApplicationException(ErrorCode.USER_NOT_FOUND, String.format("%s not founded", userName)));
    }

}
