1. We will have two Git branches, one for GUI and one for index extractor.

2. Use a mock class if you need a feature from another branch for testing. e.g. use mockPlayer in the index extractor branch or mockIndexExtractor in the GUI branch. This ensures the separation of concerns.

3. Always pull the latest changes before you start working on this feature branch. This will ensure that you're starting with the most up-to-date code.

4. Commit often and always commit after you finish a small part of this feature(e.g., several functions). It will reduce the chance of commit conflicts and help you track the team's progress.

5. Pull before pushing. This will ensure that you're not overwriting someone else's changes.

6. Communicate with your team often. This can help avoid code conflicts and ensure that everyone is on the same track.

7. Fix commit conflicts ASAP. see https://www.atlassian.com/git/tutorials/using-branches/merge-conflicts
