const agents = [
  {
    id: "sentinel",
    name: "Sentinel Warden",
    className: "Risk Sentinel",
    role: "BTP ALM Health Agent",
    tagline: "Watches the estate. Speaks risk in plain language.",
    level: 14,
    domain: "BTP / ALM",
    affinity: "Governance",
    portrait: "./assets/sentinel.jpg",
    repo: "https://github.com/srijayant/btp-alm-health-agent",
    stats: [
      { label: "Insight", value: 18 },
      { label: "Vigilance", value: 17 },
      { label: "Clarity", value: 15 },
      { label: "Speed", value: 12 },
      { label: "Restraint", value: 16 },
      { label: "Reach", value: 14 },
    ],
    abilities: [
      "Governed risk commentary across multi-app landscapes",
      "Human-in-the-loop escalation when confidence drops",
      "ALM signal weaving into narrative health briefs",
    ],
    lore: "Forged for a 100+ application BTP estate. The Warden does not chase every alert — it composes regulated risk stories operators can act on.",
  },
  {
    id: "orchestrator",
    name: "Guild Orchestrator",
    className: "Party Lead",
    role: "CrewAI Agentic SDLC",
    tagline: "Coordinates specialists through gated quests.",
    level: 13,
    domain: "AI Core / SDLC",
    affinity: "Orchestration",
    portrait: "./assets/orchestrator.jpg",
    repo: "https://github.com/srijayant/crewai-btp-sdlc",
    stats: [
      { label: "Leadership", value: 17 },
      { label: "Coordination", value: 18 },
      { label: "Craft", value: 14 },
      { label: "Speed", value: 13 },
      { label: "HITL", value: 16 },
      { label: "Scale", value: 15 },
    ],
    abilities: [
      "Multi-agent SDLC crew with role separation",
      "HITL gates between planning, build, and review",
      "BTP / AI Core deployment awareness",
    ],
    lore: "The Orchestrator keeps builders, reviewers, and validators in formation — no rogue commits past the gate without a human nod.",
  },
  {
    id: "lorekeeper",
    name: "Lorekeeper Archivist",
    className: "Knowledge Mage",
    role: "HANA Cloud RAG + AI Core",
    tagline: "Retrieves what memory alone cannot hold.",
    level: 12,
    domain: "HANA / RAG",
    affinity: "Memory",
    portrait: "./assets/lorekeeper.jpg",
    repo: "https://github.com/srijayant/hana-cloud-rag-aicore",
    stats: [
      { label: "Recall", value: 19 },
      { label: "Precision", value: 16 },
      { label: "Depth", value: 17 },
      { label: "Latency", value: 13 },
      { label: "Grounding", value: 18 },
      { label: "Synthesis", value: 15 },
    ],
    abilities: [
      "Vector retrieval over HANA Cloud knowledge",
      "Grounded answers via SAP AI Core inference",
      "Enterprise RAG patterns for regulated content",
    ],
    lore: "When the party needs truth from sprawling corpora, the Archivist opens the crystalline tome — citations first, flourish second.",
  },
  {
    id: "gatekeeper",
    name: "Seal Gatekeeper",
    className: "Security Warden",
    role: "XSUAA Multi-Tenant Kit",
    tagline: "Holds the seals between tenants.",
    level: 11,
    domain: "XSUAA / CAP",
    affinity: "Isolation",
    portrait: "./assets/gatekeeper.jpg",
    repo: "https://github.com/srijayant/btp-xsuaa-multitenant-kit",
    stats: [
      { label: "Defense", value: 18 },
      { label: "Isolation", value: 19 },
      { label: "Audit", value: 16 },
      { label: "Flexibility", value: 12 },
      { label: "Hardening", value: 17 },
      { label: "Trust", value: 15 },
    ],
    abilities: [
      "Multi-tenant security reference for CAP",
      "XSUAA binding and scope patterns",
      "Tenant isolation as a first-class mechanic",
    ],
    lore: "Every door has a seal. The Gatekeeper ensures one tenant's quest never bleeds into another's keep.",
  },
  {
    id: "inquisitor",
    name: "Validation Inquisitor",
    className: "Compliance Judge",
    role: "FDA AI Validation Playbook",
    tagline: "Asks the hard questions before go-live.",
    level: 15,
    domain: "FDA / MedTech",
    affinity: "Assurance",
    portrait: "./assets/inquisitor.jpg",
    repo: "https://github.com/srijayant/fda-ai-validation-playbook",
    stats: [
      { label: "Rigor", value: 19 },
      { label: "Evidence", value: 18 },
      { label: "Caution", value: 17 },
      { label: "Speed", value: 10 },
      { label: "Trace", value: 16 },
      { label: "Judgment", value: 18 },
    ],
    abilities: [
      "Validation playbook for agentic AI in MedTech",
      "Evidence-oriented release criteria",
      "Risk framing for FDA-aware landscapes",
    ],
    lore: "The Inquisitor does not block progress for sport — it demands a trail of proof strong enough for regulated production.",
  },
];

const rail = document.getElementById("party-rail");
const sheet = document.getElementById("sheet");
const closeBtn = document.querySelector(".close-sheet");

function renderRail() {
  rail.innerHTML = agents
    .map(
      (agent, index) => `
      <button
        class="agent-slot"
        type="button"
        role="listitem"
        data-id="${agent.id}"
        style="animation: rise 0.7s var(--ease) ${0.05 * index}s both"
        aria-pressed="false"
      >
        <img src="${agent.portrait}" alt="${agent.name} portrait" loading="lazy" />
        <div class="slot-meta">
          <p class="slot-class">${agent.className}</p>
          <p class="slot-name">${agent.name}</p>
        </div>
      </button>`
    )
    .join("");
}

function openSheet(agent) {
  document.getElementById("sheet-img").src = agent.portrait;
  document.getElementById("sheet-img").alt = `${agent.name} portrait`;
  document.getElementById("sheet-class").textContent = agent.className;
  document.getElementById("sheet-role").textContent = agent.role;
  document.getElementById("sheet-name").textContent = agent.name;
  document.getElementById("sheet-tagline").textContent = agent.tagline;
  document.getElementById("sheet-level").textContent = String(agent.level);
  document.getElementById("sheet-domain").textContent = agent.domain;
  document.getElementById("sheet-affinity").textContent = agent.affinity;
  document.getElementById("sheet-stats").innerHTML = agent.stats
    .map(
      (stat) => `
      <div class="stat">
        <strong>${stat.value}</strong>
        <span>${stat.label}</span>
      </div>`
    )
    .join("");
  document.getElementById("sheet-abilities").innerHTML = agent.abilities
    .map((ability) => `<li>${ability}</li>`)
    .join("");
  document.getElementById("sheet-lore").textContent = agent.lore;
  const link = document.getElementById("sheet-link");
  link.href = agent.repo;

  sheet.hidden = false;
  document.body.style.overflow = "hidden";
  document.querySelectorAll(".agent-slot").forEach((btn) => {
    btn.setAttribute("aria-pressed", String(btn.dataset.id === agent.id));
  });
}

function closeSheet() {
  sheet.hidden = true;
  document.body.style.overflow = "";
  document.querySelectorAll(".agent-slot").forEach((btn) => {
    btn.setAttribute("aria-pressed", "false");
  });
}

rail.addEventListener("click", (event) => {
  const slot = event.target.closest(".agent-slot");
  if (!slot) return;
  const agent = agents.find((item) => item.id === slot.dataset.id);
  if (agent) openSheet(agent);
});

closeBtn.addEventListener("click", closeSheet);
sheet.addEventListener("click", (event) => {
  if (event.target === sheet) closeSheet();
});
document.addEventListener("keydown", (event) => {
  if (event.key === "Escape" && !sheet.hidden) closeSheet();
});

renderRail();
