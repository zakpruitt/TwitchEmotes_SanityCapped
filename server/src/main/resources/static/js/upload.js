import {$, api, clear, remembered, sha256, show} from "./api.js";
import {triggerWord} from "./naming.js";

const form = $("#upload-form");
const els = {
    drop: $("#drop"),
    dropText: $("#drop-text"),
    preview: $("#preview"),
    file: $("#file"),
    name: $("#name"),
    trigger: $("#trigger"),
    chat: $("#chat-preview"),
    uploader: $("#uploader"),
    passcode: $("#passcode"),
    submit: $("#submit"),
    reset: $("#reset"),
    message: $("#message"),
};

let chosen = null;
let fileHash = null;

els.passcode.value = remembered.get("passcode");
els.uploader.value = remembered.get("uploader");

function typedName() {
    return els.name.value.trim() || (chosen ? chosen.name.replace(/\.[^.]+$/, "") : "");
}

async function refresh() {
    const name = triggerWord(typedName());

    els.trigger.innerHTML = name ? `In chat you'll type <strong>${name}</strong>` : "";
    els.chat.hidden = !(name && chosen);
    if (!els.chat.hidden) {
        els.chat.innerHTML =
            `<span>[Guild] You:</span> <img src="${els.preview.src}" alt=""> <span>${name}</span>`;
    }
    els.submit.disabled = !(chosen && name && els.uploader.value.trim() && els.passcode.value);

    if (!name && !fileHash) return;
    try {
        const params = new URLSearchParams({name, sha: fileHash ?? ""});
        const check = await api(`/api/check?${params}`);
        if (check.exact) {
            show(els.message, `That exact image is already in the pack as ${check.exact}.`, "err");
            els.submit.disabled = true;
        } else if (check.nameTaken) {
            const where = check.nameTaken === "approved" ? "already in the pack" : "already waiting for approval";
            show(els.message, `${check.name} is ${where}. Pick another name.`, "err");
            els.submit.disabled = true;
        } else {
            clear(els.message);
        }
    } catch {
        // The server checks again on submit.
    }
}

async function choose(file) {
    if (!file) return;
    chosen = file;
    fileHash = await sha256(file);
    els.preview.src = URL.createObjectURL(file);
    els.preview.hidden = false;
    els.dropText.textContent = `${file.name} · ${(file.size / 1024).toFixed(0)} KB`;
    refresh();
}

els.file.addEventListener("change", () => choose(els.file.files[0]));
els.drop.addEventListener("dragover", (event) => {
    event.preventDefault();
    els.drop.classList.add("over");
});
els.drop.addEventListener("dragleave", () => els.drop.classList.remove("over"));
els.drop.addEventListener("drop", (event) => {
    event.preventDefault();
    els.drop.classList.remove("over");
    choose(event.dataTransfer.files[0]);
});

for (const field of [els.name, els.uploader, els.passcode]) {
    field.addEventListener("input", refresh);
}

els.reset.addEventListener("click", () => {
    chosen = null;
    fileHash = null;
    form.reset();
    els.preview.hidden = true;
    els.dropText.textContent = "Drop an image here, or click to pick one";
    clear(els.message);
    refresh();
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    els.submit.disabled = true;
    remembered.set("passcode", els.passcode.value);
    remembered.set("uploader", els.uploader.value.trim());

    const body = new FormData();
    body.append("file", chosen);
    body.append("name", typedName());
    body.append("uploader", els.uploader.value.trim());

    try {
        const queued = await api("/api/emotes", {method: "POST", body, passcode: els.passcode.value});
        els.reset.click();
        show(els.message, `${queued.name} is in the queue. It'll show up in the addon once it's approved.`);
    } catch (error) {
        show(els.message, error.message, "err");
        els.submit.disabled = false;
    }
});

refresh();
