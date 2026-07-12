(Π-state
  (timestamp "2026-07-12T17:30:00Z")
  (branch "main")
  (base-commit "e92e389")
  (staged-count 34)
  (unstaged
    (docs/inbox/.#2026.07.12.10.17.57.md "Emacs lockfile symlink — not committed"))
  (untracked-count 0)
  (untracked ())
  (test-result
    (total 528)
    (assertions 1336)
    (failures 9)
    (regressions
      (registration-shape "registration service now returns observation-wrapped records; tests expect flat map"
        (tests
          epiphany.application.registration-test/registers-a-new-repository-with-a-new-resource-id
          epiphany.application.registration-test/registration-reuses-an-existing-git-local-resource-id
          epiphany.application.registration-test/registration-observation-is-idempotent-by-request-id))
      (profile-shape "profile bootstrap returns observation-wrapped record"
        (tests epiphany.infra.profile-test/bootstrap-local-mode-idempotent-by-request-id))
      (http-register "POST /api/v1/register returns 500"
        (tests
          epiphany.infra.http-test/router-handles-register-post
          epiphany.infra.http-test/exception-returns-problem-json)))))
