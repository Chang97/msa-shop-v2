package com.msashop.auth.application.port.out;

import java.util.List;
import java.util.Optional;

public interface LoadUserPort {
    Optional<AuthUserRecord> findByLoginId(String loginId);
    /**
     * application ?덉씠?댁뿉???꾩슂??留뚰겮留??ㅺ퀬 ?ㅻ뒗 "議고쉶 ?꾩슜 ?덉퐫??
     * (?꾨찓??紐⑤뜽???꾩쭅 ?뺤젙 ???덉쑝硫???諛⑹떇??媛???⑥닚)
     */
    record AuthUserRecord(Long userId, String email, String loginId, String passwordHash, Boolean enabled, List<String> roles) { }

}

