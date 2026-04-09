package com.hubilon.modules.folder.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

import static lombok.AccessLevel.PROTECTED;

@Getter
@Entity
@Table(name = "folder_members")
@NoArgsConstructor(access = PROTECTED)
public class FolderMemberJpaEntity {

    @EmbeddedId
    private FolderMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("folderId")
    @JoinColumn(name = "folder_id")
    private FolderJpaEntity folder;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    @Builder
    public FolderMemberJpaEntity(FolderJpaEntity folder, Long userId) {
        this.id = new FolderMemberId(folder.getId(), userId);
        this.folder = folder;
        this.userId = userId;
    }

    @Embeddable
    public static class FolderMemberId implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "folder_id")
        private Long folderId;

        @Column(name = "user_id")
        private Long userId;

        protected FolderMemberId() {}

        public FolderMemberId(Long folderId, Long userId) {
            this.folderId = folderId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FolderMemberId that)) return false;
            return Objects.equals(folderId, that.folderId) &&
                   Objects.equals(userId, that.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(folderId, userId);
        }
    }
}
