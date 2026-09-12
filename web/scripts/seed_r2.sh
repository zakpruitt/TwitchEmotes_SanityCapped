#!/usr/bin/env bash
set -e
# Pass --local to target the local dev bucket instead of the real one.
REMOTE="${1:---remote}"
npx wrangler r2 object put sanity-capped-emotes/emotes/0a0ce337-f25f-452f-b3f4-d7fe1e121e86.png --file "tools/source/scCloudzUlt.png" --content-type "image/png" $REMOTE
npx wrangler r2 object put sanity-capped-emotes/emotes/9d56efff-f4a6-4e6e-b09f-66e570813795.png --file "tools/source/scFuck.png" --content-type "image/png" $REMOTE
npx wrangler r2 object put sanity-capped-emotes/emotes/b121c5e2-196f-43b8-bf3e-7d071895eeb1.gif --file "tools/source/scImIn.gif" --content-type "image/gif" $REMOTE
npx wrangler r2 object put sanity-capped-emotes/emotes/225fc5b2-35bc-47ef-be64-cbce202570f7.gif --file "tools/source/scOooo.gif" --content-type "image/gif" $REMOTE
npx wrangler r2 object put sanity-capped-emotes/emotes/ea6931dd-fbea-40a3-bd2c-a6e45722a544.png --file "tools/source/scPewW.png" --content-type "image/png" $REMOTE
npx wrangler r2 object put sanity-capped-emotes/emotes/88404d69-146b-44ab-bd47-37c5e0cc5c3a.jpg --file "tools/source/scTheChosen.jpg" --content-type "image/jpeg" $REMOTE
npx wrangler r2 object put sanity-capped-emotes/emotes/8aae698a-a57e-4ac8-a849-ce448474d9ca.png --file "tools/source/scTrue.png" --content-type "image/png" $REMOTE
npx wrangler r2 object put sanity-capped-emotes/emotes/c16d9797-1e2c-4684-b441-0c37804834c8.png --file "tools/source/scTurtleKillOrder.png" --content-type "image/png" $REMOTE
npx wrangler r2 object put sanity-capped-emotes/emotes/5ce3d65f-7c8f-4f54-a90a-1373439b8c42.png --file "tools/source/scVynChatting.png" --content-type "image/png" $REMOTE
npx wrangler r2 object put sanity-capped-emotes/emotes/c030664c-133a-416f-8b87-a484f56daaa8.gif --file "tools/source/scWeAreGathered.gif" --content-type "image/gif" $REMOTE
