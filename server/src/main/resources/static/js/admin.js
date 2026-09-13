import { $, api, clear, postJson, remembered, show } from "./api.js";

const gate = $("#gate");
const panel = $("#panel");
const queue = $("#queue");
const approved = $("#approved");
const passcodeField = $("#passcode");

let passcode = remembered.get("adminPasscode");

gate.addEventListener("submit", (event) => {
  event.preventDefault();
  unlock({ quiet: false });
});

$("#refresh").addEventListener("click", () => load());
$("#resync").addEventListener("click", async (event) => {
  event.target.disabled = true;
  try {
    const { added } = await postJson("/api/admin/resync", {}, passcode);
    show($("#gate-message"), added ? `Adopted ${added} emote(s) from the repo.` : "Already up to date.");
    await load();
  } catch (error) {
    alert(error.message);
  }
  event.target.disabled = false;
});

async function unlock({ quiet }) {
  passcode = passcodeField.value || passcode;
  if (!passcode) return;
  try {
    const session = await api("/api/session", { passcode });
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
  const [{ pending }, { emotes }] = await Promise.all([
    api("/api/admin/pending", { passcode }),
    api("/api/emotes"),
  ]);

  $("#pending-count").textContent = `${pending.length} waiting`;
  queue.innerHTML = pending.length
    ? pending.map(pendingCard).join("")
    : `<p class="empty">Nothing waiting. Quiet guild.</p>`;
  approved.innerHTML = emotes.map(approvedCard).join("");
}

function pendingCard(emote) {
  return `
    <div class="card pending" data-id="${emote.id}">
      <img src="${emote.url}" alt="${emote.name}">
      <div class="meta">
        <div class="name">${emote.name}</div>
        <div class="by">by ${emote.uploader} · ${emote.age}</div>
        ${emote.note ? `<span class="tag warn">${emote.note}</span>` : ""}
        <p class="chatline"><span>[Guild]</span><img src="${emote.url}" alt=""><span>${emote.name}</span></p>
      </div>
      <div class="actions">
        <input type="text" data-rename placeholder="rename (optional)">
        <button class="ok" data-approve>Approve</button>
        <button class="danger" data-reject>Reject</button>
      </div>
    </div>`;
}

function approvedCard(emote) {
  return `
    <div class="emote">
      <img src="${emote.url}" alt="${emote.name}">
      <span class="name">${emote.name}</span>
      <span class="by">by ${emote.uploader}</span>
      <button class="danger small" data-remove="${emote.id}" data-name="${emote.name}">Remove</button>
    </div>`;
}

queue.addEventListener("click", async (event) => {
  const card = event.target.closest(".pending");
  if (!card) return;
  const id = card.dataset.id;
  const rename = $("[data-rename]", card).value.trim();

  if (event.target.matches("[data-approve]")) {
    event.target.disabled = true;
    event.target.textContent = "Committing…";
    try {
      const decided = await postJson(`/api/admin/${id}/approve`, { name: rename }, passcode);
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
      await postJson(`/api/admin/${id}/reject`, { reason }, passcode);
      await load();
    } catch (error) {
      alert(error.message);
    }
  }
});

approved.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-remove]");
  if (!button) return;
  if (!confirm(`Remove ${button.dataset.name} from the pack? This publishes an update without it.`)) return;
  button.disabled = true;
  try {
    await api(`/api/admin/${button.dataset.remove}`, { method: "DELETE", passcode });
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
  unlock({ quiet: true });
}
