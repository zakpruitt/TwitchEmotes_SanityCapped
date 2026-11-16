import {$, api, clear, postJson, remembered, show} from "./api.js";

const gate = $("#gate");
const panel = $("#panel");
const queue = $("#queue");
const approved = $("#approved");
const approvedSounds = $("#approved-sounds");
const passcodeField = $("#passcode");

let passcode = remembered.get("adminPasscode");

/** Where each kind's admin endpoints live. */
const ADMIN_PATH = {emote: "/api/admin", sound: "/api/admin/sounds"};

/** Uploader names are free text, so nothing from the server goes in unescaped. */
function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, (c) => (
        {"&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"}[c]));
}

gate.addEventListener("submit", (event) => {
    event.preventDefault();
    unlock({quiet: false});
});

$("#refresh").addEventListener("click", () => load());
$("#resync").addEventListener("click", async (event) => {
    event.target.disabled = true;
    try {
        const {added} = await postJson("/api/admin/resync", {}, passcode);
        show($("#gate-message"), added ? `Adopted ${added} file(s) from the repo.` : "Already up to date.");
        await load();
    } catch (error) {
        alert(error.message);
    }
    event.target.disabled = false;
});

async function unlock({quiet}) {
    passcode = passcodeField.value || passcode;
    if (!passcode) return;
    try {
        const session = await api("/api/session", {passcode});
        if (!session.admin) throw new Error("That passcode isn't the admin one.");
        remembered.set("adminPasscode", passcode);
        clear($("#gate-message"));
        gate.hidden = true;
        panel.hidden = false;
        await load();
    } catch (error) {
        if (!quiet) show($("#gate-message"), error.message, "err");
    }
}

async function load() {
    const [emoteQueue, {emotes}, soundQueue, {sounds}] = await Promise.all([
        api("/api/admin/pending", {passcode}),
        api("/api/emotes"),
        api("/api/admin/sounds/pending", {passcode}),
        api("/api/sounds"),
    ]);

    const waiting = emoteQueue.pending.length + soundQueue.pending.length;
    $("#pending-count").textContent = `${waiting} waiting`;
    queue.innerHTML = waiting
        ? emoteQueue.pending.map(pendingEmote).join("") + soundQueue.pending.map(pendingSound).join("")
        : `<p class="empty">Nothing waiting. Quiet guild.</p>`;
    approved.innerHTML = emotes.map(approvedEmote).join("");
    approvedSounds.innerHTML = sounds.length
        ? sounds.map(approvedSound).join("")
        : `<p class="empty">No sounds yet.</p>`;
}

function decisionButtons() {
    return `
      <div class="actions">
        <input type="text" data-rename placeholder="rename (optional)">
        <button class="ok" data-approve>Approve</button>
        <button class="danger" data-reject>Reject</button>
      </div>`;
}

function pendingEmote(emote) {
    return `
    <div class="card pending" data-id="${esc(emote.id)}" data-kind="emote">
      <img src="${esc(emote.url)}" alt="${esc(emote.name)}">
      <div class="meta">
        <div class="name">${esc(emote.name)}</div>
        <div class="by">emote · by ${esc(emote.uploader)} · ${esc(emote.age)}</div>
        ${emote.note ? `<span class="tag warn">${esc(emote.note)}</span>` : ""}
        <p class="chatline"><span>[Guild]</span><img src="${esc(emote.url)}" alt=""><span>${esc(emote.name)}</span></p>
      </div>
      ${decisionButtons()}
    </div>`;
}

function pendingSound(sound) {
    return `
    <div class="card pending" data-id="${esc(sound.id)}" data-kind="sound">
      <div class="meta">
        <div class="name">${esc(sound.name)} <span class="cmd">${esc(sound.command)}</span></div>
        <div class="by">sound · by ${esc(sound.uploader)} · ${esc(sound.seconds)}s · ${esc(sound.age)}</div>
        <audio controls preload="none" src="${esc(sound.url)}"></audio>
      </div>
      ${decisionButtons()}
    </div>`;
}

function approvedEmote(emote) {
    return `
    <div class="emote">
      <img src="${esc(emote.url)}" alt="${esc(emote.name)}">
      <span class="name">${esc(emote.name)}</span>
      <span class="by">by ${esc(emote.uploader)}</span>
      <button class="danger small" data-remove="${esc(emote.id)}" data-kind="emote" data-name="${esc(emote.name)}">Remove</button>
    </div>`;
}

function approvedSound(sound) {
    return `
    <div class="emote sound">
      <span class="name">${esc(sound.name)}</span>
      <span class="by">by ${esc(sound.uploader)} · ${esc(sound.seconds)}s</span>
      <audio controls preload="none" src="${esc(sound.url)}"></audio>
      <button class="danger small" data-remove="${esc(sound.id)}" data-kind="sound" data-name="${esc(sound.name)}">Remove</button>
    </div>`;
}

queue.addEventListener("click", async (event) => {
    const card = event.target.closest(".pending");
    if (!card) return;
    const path = `${ADMIN_PATH[card.dataset.kind]}/${card.dataset.id}`;
    const rename = $("[data-rename]", card).value.trim();

    if (event.target.matches("[data-approve]")) {
        event.target.disabled = true;
        event.target.textContent = "Committing…";
        try {
            const decided = await postJson(`${path}/approve`, {name: rename}, passcode);
            card.replaceWith(note(`${decided.name} approved — the release is building.`));
            await load();
        } catch (error) {
            event.target.disabled = false;
            event.target.textContent = "Approve";
            alert(error.message);
        }
    }

    if (event.target.matches("[data-reject]")) {
        const reason = prompt("Reason (optional, just for your own records):") ?? "";
        try {
            await postJson(`${path}/reject`, {reason}, passcode);
            await load();
        } catch (error) {
            alert(error.message);
        }
    }
});

panel.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-remove]");
    if (!button) return;
    if (!confirm(`Remove ${button.dataset.name} from the pack? This publishes an update without it.`)) return;
    button.disabled = true;
    try {
        await api(`${ADMIN_PATH[button.dataset.kind]}/${button.dataset.remove}`, {method: "DELETE", passcode});
        await load();
    } catch (error) {
        button.disabled = false;
        alert(error.message);
    }
});

function note(text) {
    const element = document.createElement("p");
    element.className = "msg ok";
    element.textContent = text;
    return element;
}

if (passcode) {
    passcodeField.value = passcode;
    unlock({quiet: true});
}
