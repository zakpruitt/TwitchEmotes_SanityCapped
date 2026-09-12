/** Serves uploaded originals out of R2. Content-addressed by id, so cache hard. */
export async function onRequestGet({ env, params }) {
  const key = `emotes/${Array.isArray(params.path) ? params.path.join("/") : params.path}`;
  const object = await env.BUCKET.get(key);
  if (!object) return new Response("Not found", { status: 404 });

  const headers = new Headers();
  object.writeHttpMetadata(headers);
  headers.set("etag", object.httpEtag);
  headers.set("cache-control", "public, max-age=31536000, immutable");
  return new Response(object.body, { headers });
}
