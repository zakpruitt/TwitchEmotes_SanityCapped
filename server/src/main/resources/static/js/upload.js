import {$, api, clear, remembered, sha256, show} from "./api.js";
import {command, soundName, triggerWord} from "./naming.js";

const DROP_PROMPT = "Drop an image or an MP3 here, or click to pick one";

const form = $("#upload-form");
const maxSoundSeconds = Number(form.dataset.maxSoundSeconds);
const els = {
    drop: $("#drop"),
    dropText: $("#drop-text"),
    preview: $("#preview"),
    listen: $("#listen"),
    file: $("#file"),
    name: $("#name"),
    trigger: $("#trigger"),
    chat: $("#chat-preview"),
    uploader: $("#uploader"),
    submit: $("#submit"),
    reset: $("#reset"),
    message: $("#message"),
    sent: $("#sent"),
};

let chosen = null;
let fileHash = null;
let tooLong = false;

els.uploader.value = remembered.get("uploader");

/** An MP3 is a sound; anything else is treated as an emote and the server decides. */
function isSound() {
    return Boolean(chosen) && (chosen.type === "audio/mpeg" || /\.mp3$/i.test(chosen.name));
}

function typedName() {
    return els.name.value.trim() || (chosen ? chosen.name.replace(/\.[^.]+$/, "") : "");
}

async function refresh() {
    const sound = isSound();
    const name = sound ? soundName(typedName()) : triggerWord(typedName());

    if (!name) {
        els.trigger.innerHTML = "";
    } else if (sound) {
        els.trigger.innerHTML =
            `In game you'll type <strong>${command(name)}</strong>, or pick <strong>${name}</strong> in Soundboard`;
    } else {
        els.trigger.innerHTML = `In chat you'll type <strong>${name}</strong>`;
    }

    els.chat.hidden = sound || !(name && chosen);
    if (!els.chat.hidden) {
        els.chat.innerHTML =
            `<span>[Guild] You:</span> <img src="${els.preview.src}" alt=""> <span>${name}</span>`;
    }
    els.submit.disabled = !(chosen && name && els.uploader.value.trim()) || tooLong;

    if (tooLong) {
        show(els.message, `That sound is too long. Keep it under ${maxSoundSeconds} seconds.`, "err");
        return;
    }
    if (!name && !fileHash) return;
    try {
        const params = new URLSearchParams({name, sha: fileHash ?? ""});
        const check = await api(`${sound ? "/api/sounds/check" : "/api/check"}?${params}`);
        if (check.exact) {
            show(els.message, `That exact file is already in the pack as ${check.exact}.`, "err");
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
    tooLong = false;
    clear(els.sent);
    fileHash = await sha256(file);

    const url = URL.createObjectURL(file);
    const size = `${file.name} · ${(file.size / 1024).toFixed(0)} KB`;
    els.dropText.textContent = size;

    if (isSound()) {
        els.preview.hidden = true;
        els.listen.src = url;
        els.listen.hidden = false;
        els.listen.onloadedmetadata = () => {
            const seconds = els.listen.duration;
            if (!Number.isFinite(seconds)) return;
            els.dropText.textContent = `${size} · ${seconds.toFixed(1)}s`;
            tooLong = seconds > maxSoundSeconds;
            refresh();
        };
    } else {
        els.listen.hidden = true;
        els.listen.removeAttribute("src");
        els.preview.src = url;
        els.preview.hidden = false;
    }
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

for (const field of [els.name, els.uploader]) {
    field.addEventListener("input", refresh);
}

els.reset.addEventListener("click", () => {
    chosen = null;
    fileHash = null;
    tooLong = false;
    form.reset();
    els.uploader.value = remembered.get("uploader");
    els.preview.hidden = true;
    els.listen.pause();
    els.listen.hidden = true;
    els.listen.removeAttribute("src");
    els.dropText.textContent = DROP_PROMPT;
    clear(els.message);
    refresh();
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    els.submit.disabled = true;
    remembered.set("uploader", els.uploader.value.trim());

    const sound = isSound();
    const body = new FormData();
    body.append("file", chosen);
    body.append("name", typedName());
    body.append("uploader", els.uploader.value.trim());

    try {
        const queued = await api(sound ? "/api/sounds" : "/api/emotes", {method: "POST", body});
        els.reset.click();
        show(els.sent, `Request sent! ${queued.name} is in the queue and will show up in the addon once it's approved.`);
        window.scrollTo({top: 0, behavior: "smooth"});
    } catch (error) {
        show(els.message, error.message, "err");
        els.submit.disabled = false;
    }
});

refresh();
