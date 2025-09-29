// JWTRefreshTokenRotationExample.java
// Demonstrates secure refresh token rotation with detection of token theft.
// Strategy: Issue short-lived access token + one-time-use refresh token pair.
// On refresh: invalidate old refresh token, issue new pair; if old used twice => revoke session chain.

import java.time.Instant;
import java.util.*;

public class JWTRefreshTokenRotationExample {
    static class SessionChain {
        final String subject;
        String currentRefreshId; // latest valid refresh token id
        boolean revoked = false;
        final Deque<String> history = new ArrayDeque<>();
        SessionChain(String subject, String rid) {
            this.subject = subject; this.currentRefreshId = rid; history.add(rid);
        }
    }

    static class TokenStore {
        private final Map<String, SessionChain> chains = new HashMap<>();
        private final Map<String, String> refreshToSession = new HashMap<>();

        synchronized SessionChain create(String subject) {
            String rid = UUID.randomUUID().toString();
            var chain = new SessionChain(subject, rid);
            chains.put(chain.subject, chain);
            refreshToSession.put(rid, chain.subject);
            return chain;
        }

        synchronized Optional<SessionChain> consumeRefresh(String refreshId) {
            String subject = refreshToSession.get(refreshId);
            if (subject == null) return Optional.empty();
            var chain = chains.get(subject);
            if (chain == null || chain.revoked) return Optional.empty();
            if (!chain.currentRefreshId.equals(refreshId)) {
                // Replay attempt => revoke chain
                chain.revoked = true;
                return Optional.empty();
            }
            // Rotate
            String newRid = UUID.randomUUID().toString();
            chain.currentRefreshId = newRid;
            chain.history.add(newRid);
            refreshToSession.put(newRid, subject);
            refreshToSession.remove(refreshId);
            return Optional.of(chain);
        }
    }

    record AccessToken(String jwt, Instant exp) {}
    record RefreshToken(String id, Instant exp) {}

    static class AuthService {
        private final TokenStore store = new TokenStore();
        private final long accessSeconds = 60; // short lived
        private final long refreshSeconds = 300; // moderate window

        record TokenPair(AccessToken access, RefreshToken refresh) {}

        TokenPair login(String user) {
            var chain = store.create(user);
            return issuePair(chain.currentRefreshId, user);
        }

        Optional<TokenPair> refresh(String refreshId) {
            var chainOpt = store.consumeRefresh(refreshId);
            if (chainOpt.isEmpty()) return Optional.empty();
            var chain = chainOpt.get();
            if (chain.revoked) return Optional.empty();
            return Optional.of(issuePair(chain.currentRefreshId, chain.subject));
        }

        private TokenPair issuePair(String refreshId, String subject) {
            var now = Instant.now();
            var access = new AccessToken("access.jwt.for." + subject, now.plusSeconds(accessSeconds));
            var refresh = new RefreshToken(refreshId, now.plusSeconds(refreshSeconds));
            return new TokenPair(access, refresh);
        }
    }

    public static void main(String[] args) {
        var auth = new AuthService();
        var pair1 = auth.login("alice");
        System.out.println("Login access=" + pair1.access().jwt());

        var pair2 = auth.refresh(pair1.refresh().id()).orElseThrow();
        System.out.println("Rotated refresh -> new access=" + pair2.access().jwt());

        // Malicious reuse of old refresh id triggers revocation
        var compromised = auth.refresh(pair1.refresh().id());
        System.out.println("Replay attempt accepted? " + compromised.isPresent());

        // Further legitimate refresh fails now
        var afterRevocation = auth.refresh(pair2.refresh().id());
        System.out.println("Post-revocation refresh success? " + afterRevocation.isPresent());
    }
}
