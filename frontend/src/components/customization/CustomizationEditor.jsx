const PRESET_THEMES = [
  { id: "minimal", name: "極簡", color: "#3b82f6", desc: "乾淨現代，適合多數服務業" },
  { id: "elegant", name: "質感", color: "#8b5cf6", desc: "高級感風格，適合美業與精品" },
  { id: "clinic", name: "醫療", color: "#0ea5a4", desc: "專業可信，適合醫療與保健" },
  { id: "fitness", name: "活力", color: "#f97316", desc: "動感鮮明，適合健身與運動" },
];

export function CustomizationEditor({ customization, onChange, onSave }) {
  if (!customization) return null;

  function applyPreset(preset) {
    onChange((v) => ({ ...v, themePreset: preset.id, themeColor: preset.color }));
  }

  return (
    <form onSubmit={onSave} className="form-grid">
      <label className="full">
        品牌主題
        <select
          value={customization.themePreset || "minimal"}
          onChange={(e) => {
            const selected = PRESET_THEMES.find((item) => item.id === e.target.value);
            if (selected) applyPreset(selected);
          }}
        >
          {PRESET_THEMES.map((theme) => (
            <option key={theme.id} value={theme.id}>{theme.name}</option>
          ))}
        </select>
      </label>

      <div className="theme-preset-grid full">
        {PRESET_THEMES.map((theme) => (
          <button
            key={theme.id}
            type="button"
            className={`theme-preset-card ${customization.themePreset === theme.id ? "active" : ""}`}
            onClick={() => applyPreset(theme)}
          >
            <span className="theme-dot" style={{ background: theme.color }} />
            <strong>{theme.name}</strong>
            <small>{theme.desc}</small>
          </button>
        ))}
      </div>

      <label>
        主題色
        <input
          value={customization.themeColor}
          onChange={(e) => onChange((v) => ({ ...v, themeColor: e.target.value }))}
          placeholder="#3b82f6"
        />
      </label>
      <label>
        首頁標題
        <input
          value={customization.heroTitle}
          onChange={(e) => onChange((v) => ({ ...v, heroTitle: e.target.value }))}
          placeholder="Your store title"
        />
      </label>
      <label className="full">
        預約流程文案
        <input
          value={customization.bookingFlowText}
          onChange={(e) => onChange((v) => ({ ...v, bookingFlowText: e.target.value }))}
          placeholder="請說明預約流程"
        />
      </label>
      <label className="full">
        邀請碼（顧客送出預約時必填）
        <input
          value={customization.inviteCode || ""}
          onChange={(e) => onChange((v) => ({ ...v, inviteCode: e.target.value }))}
          placeholder="例如：VIP2026"
        />
      </label>
      <label className="full">
        服務條款
        <input
          value={customization.termsText || ""}
          onChange={(e) => onChange((v) => ({ ...v, termsText: e.target.value }))}
          placeholder="請輸入服務條款與注意事項"
        />
      </label>
      <label className="full">
        公告文字
        <input
          value={customization.announcementText || ""}
          onChange={(e) => onChange((v) => ({ ...v, announcementText: e.target.value }))}
          placeholder="顯示於使用者預約頁面的公告"
        />
      </label>
      <label>
        預約緩衝時間（分鐘）
        <input
          type="number"
          min="0"
          value={customization.bufferMinutes ?? 0}
          onChange={(e) => onChange((v) => ({ ...v, bufferMinutes: Number(e.target.value || 0) }))}
          placeholder="15"
        />
      </label>
      <label>
        FAQ（JSON）
        <input
          value={customization.faqJson || "[]"}
          onChange={(e) => onChange((v) => ({ ...v, faqJson: e.target.value }))}
          placeholder='[{"q":"...","a":"..."}]'
        />
      </label>
      <label>
        首頁區塊（JSON）
        <input
          value={customization.homepageSectionsJson}
          onChange={(e) => onChange((v) => ({ ...v, homepageSectionsJson: e.target.value }))}
          placeholder='["hero","services","booking"]'
        />
      </label>
      <label>
        分類排序（JSON）
        <input
          value={customization.categoryOrderJson}
          onChange={(e) => onChange((v) => ({ ...v, categoryOrderJson: e.target.value }))}
          placeholder='["General","Beauty"]'
        />
      </label>
      <button type="submit" className="btn btn-primary full">
        儲存客製化設定
      </button>
    </form>
  );
}
