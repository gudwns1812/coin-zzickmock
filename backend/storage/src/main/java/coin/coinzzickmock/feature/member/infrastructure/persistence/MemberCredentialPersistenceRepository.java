package coin.coinzzickmock.feature.member.infrastructure.persistence;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.member.application.repository.MemberCredentialRepository;
import coin.coinzzickmock.feature.member.domain.MemberCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberCredentialPersistenceRepository implements MemberCredentialRepository {
    private final MemberCredentialEntityRepository memberCredentialEntityRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberCredential> findActiveByMemberId(Long memberId) {
        return memberCredentialEntityRepository.findByIdAndWithdrawnAtIsNull(memberId)
                .map(MemberCredentialEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberCredential> findActiveByAccount(String account) {
        return memberCredentialEntityRepository.findByAccountAndWithdrawnAtIsNull(account)
                .map(MemberCredentialEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MemberCredential> findByAccountIncludingWithdrawn(String account) {
        return memberCredentialEntityRepository.findByAccount(account)
                .map(MemberCredentialEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByAccount(String account) {
        return memberCredentialEntityRepository.existsByAccount(account);
    }

    @Override
    @Transactional
    public MemberCredential create(MemberCredential memberCredential) {
        try {
            return memberCredentialEntityRepository.saveAndFlush(MemberCredentialEntity.from(memberCredential))
                    .toDomain();
        } catch (DataIntegrityViolationException exception) {
            if (isAccountUniqueViolation(exception)) {
                throw new CoreException(ErrorCode.MEMBER_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

    @Override
    @Transactional
    public MemberCredential save(MemberCredential memberCredential) {
        if (memberCredential.memberId() == null) {
            return create(memberCredential);
        }

        MemberCredentialEntity entity = memberCredentialEntityRepository.findById(memberCredential.memberId())
                .map(existing -> {
                    existing.apply(memberCredential);
                    return existing;
                })
                .orElseGet(() -> MemberCredentialEntity.from(memberCredential));
        return memberCredentialEntityRepository.save(entity).toDomain();
    }

    private boolean isAccountUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        if (message == null) {
            return false;
        }
        String normalizedMessage = message.toLowerCase();
        return normalizedMessage.contains("uk_member_credentials_account")
                || ((normalizedMessage.contains("unique") || normalizedMessage.contains("duplicate"))
                && normalizedMessage.contains("member_credentials")
                && normalizedMessage.contains("account"));
    }

}
