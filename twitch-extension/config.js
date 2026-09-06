/**
 * Where the game lives.
 *
 * A Twitch extension is uploaded to Twitch as a zip and served from their domain, so it
 * has no idea which server it belongs to — this is the one thing that has to be told.
 * Change it to your own host before zipping, and declare that same host in the developer
 * console, under "Allowlist for URL Fetching Domains": the panel runs under a CSP that
 * blocks anything it has not been promised.
 */
window.CARDGAME_API = 'https://sans-filtre.example'
