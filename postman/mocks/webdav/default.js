const http = require("http");

// RFC 4918 stand-in for a user-selected server; test credentials contain no real secrets.
const expectedAuth = "Basic " + Buffer.from("test-user:test-password").toString("base64");
const escapeXml = value => value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/"/g, "&quot;");
const entry = (href, collection, size = 0) =>
    `<d:response><d:href>${escapeXml(href)}</d:href><d:propstat><d:prop>` +
    `<d:resourcetype>${collection ? "<d:collection/>" : ""}</d:resourcetype>` +
    `<d:getcontentlength>${size}</d:getcontentlength>` +
    `<d:getlastmodified>Fri, 30 Oct 2026 00:00:00 GMT</d:getlastmodified>` +
    `</d:prop><d:status>HTTP/1.1 200 OK</d:status></d:propstat></d:response>`;

http.createServer(async (req, res) => {
    if (req.headers.authorization !== expectedAuth) {
        res.writeHead(401, { "WWW-Authenticate": 'Basic realm="WebDAV"' });
        return res.end();
    }
    const match = req.url.split("?")[0].match(/^\/([^/]+)\/XposedSmsCode(?:\/(.*))?$/);
    if (!match) {
        res.writeHead(501);
        return res.end();
    }
    const [, scope, rawName = ""] = match;
    const name = decodeURIComponent(rawName);
    const stateKey = "webdav." + scope;
    const state = await pm.state.get(stateKey) || { exists: false, files: {} };
    const end = (code, body = "") => { res.writeHead(code); res.end(body); };
    if (scope === "broken-download" && req.method === "GET") return end(500, "<html>Unavailable</html>");
    if (req.method === "MKCOL" && !name) {
        if (state.exists) return end(405);
        state.exists = true;
        await pm.state.set(stateKey, state);
        return end(201);
    }
    if (req.method === "PROPFIND" && !name) {
        if (!state.exists) return end(404);
        let entries = entry("./", true);
        if (req.headers.depth === "1") {
            entries += Object.entries(state.files).map(([key, data]) =>
                entry(encodeURIComponent(key), false, Buffer.from(data, "base64").length)).join("");
        } else if (req.headers.depth !== "0") return end(400);
        res.setHeader("Content-Type", "application/xml; charset=utf-8");
        return end(207, `<d:multistatus xmlns:d="DAV:">${entries}</d:multistatus>`);
    }
    if (req.method === "PUT" && name && !name.includes("/")) {
        if (!state.exists) return end(409);
        if (scope === "quota") return end(507);
        if (state.files[name] && req.headers["if-none-match"] === "*") return end(412);
        const chunks = [];
        // Postman interception supplies req.body; native streaming supports direct local use as well.
        const store = async body => {
            state.files[name] = Buffer.from(body).toString("base64");
            await pm.state.set(stateKey, state);
            end(201);
        };
        if (req.body !== undefined) {
            return store(Buffer.isBuffer(req.body) ? req.body :
                typeof req.body === "string" ? req.body : JSON.stringify(req.body));
        }
        req.on("data", chunk => chunks.push(chunk));
        req.on("end", () => store(Buffer.concat(chunks)));
        return;
    }
    if (req.method === "GET" && name) {
        if (!state.files[name]) return end(404);
        return end(200, Buffer.from(state.files[name], "base64"));
    }
    end(501);
}).listen(process.env.PORT || 4500);
