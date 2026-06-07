// =============================================================================
// Esprit Market — Store Configuration
// =============================================================================
// Centralise toutes les catégories, thèmes visuels et la gestion des profils
// de boutiques dans le localStorage.
// =============================================================================

// ---------------------------------------------------------------------------
// Types & Interfaces
// ---------------------------------------------------------------------------

/** Identifiants uniques de chaque catégorie de boutique. */
export type StoreCategoryId =
  | 'CLOTHING_BRAND'
  | 'ACCESSORIES'
  | 'ART_PAINTINGS'
  | 'BAGS'
  | 'PASTRY'
  | 'SPORTS_CLOTHING'
  | 'NOTEBOOKS'
  | 'MUGS';

/** Entrée dans la liste déroulante de sélection de catégorie. */
export interface StoreCategoryOption {
  id: StoreCategoryId;
  label: string;
}

/** Palette visuelle et typographie associées à une catégorie. */
export interface StoreTheme {
  /** Identifiant du thème (slug). */
  id: string;
  /** Nom d'affichage du thème. */
  name: string;
  /** Courte description de l'ambiance. */
  vibe: string;
  /** Label de la catégorie associée. */
  categoryLabel: string;
  /** Dégradé principal de fond de page. */
  background: string;
  /** Fond adouci pour les sections secondaires. */
  backgroundSoft: string;
  /** Halo décoratif (radial-gradient). */
  decorativeGlow: string;
  /** Overlay semi-transparent sur les images héros. */
  heroOverlay: string;
  /** Fond des cartes produit. */
  cardBackground: string;
  /** Couleur de texte principale. */
  text: string;
  /** Couleur de texte secondaire / légende. */
  mutedText: string;
  /** Couleur d'action principale (boutons, liens). */
  primary: string;
  /** Variante plus sombre de la couleur principale. */
  primaryStrong: string;
  /** Couleur d'accentuation (badges, highlights). */
  accent: string;
  /** Police des titres (valeur CSS font-family). */
  headingFont: string;
  /** Police du corps de texte (valeur CSS font-family). */
  bodyFont: string;
}

/** Profil complet d'une boutique créée par un vendeur. */
export interface CreatedStoreProfile {
  id?: string;
  name: string;
  categoryId: StoreCategoryId;
  categoryLabel: string;
  phone: string;
  description: string;
  logoDataUrl: string;
  themeId: string;
  ownerName?: string;
  ownerEmail?: string;
  ownerId?: string;
  createdAt: string;
  /** Solde en dinars (optionnel, usage interne). */
  balance?: number;
}

/** Profil public d'une boutique (visible par les visiteurs). */
export interface PublicStoreProfile extends CreatedStoreProfile {
  /** Identifiant unique dérivé du nom normalisé. */
  id?: string;
}

// ---------------------------------------------------------------------------
// Clés localStorage
// ---------------------------------------------------------------------------

export const CREATED_STORE_PROFILE_KEY = 'create_store_latest_shop_profile';
export const PUBLIC_STORE_PROFILES_KEY  = 'esprit_market_public_store_profiles';

// ---------------------------------------------------------------------------
// Catégories disponibles
// ---------------------------------------------------------------------------

export const STORE_CATEGORY_OPTIONS: readonly StoreCategoryOption[] = [
  { id: 'CLOTHING_BRAND',  label: 'Marque de vêtements'                          },
  { id: 'ACCESSORIES',     label: 'Accessoires'                                   },
  { id: 'ART_PAINTINGS',   label: 'Tableaux & Art'                                },
  { id: 'BAGS',            label: 'Sacs'                                          },
  { id: 'PASTRY',          label: 'Pâtisserie (petits gâteaux & produits sains)'  },
  { id: 'SPORTS_CLOTHING', label: 'Vêtements de sport'                            },
  { id: 'NOTEBOOKS',       label: 'Cahiers personnalisés'                         },
  { id: 'MUGS',            label: 'Tasses personnalisées'                         },
];

// ---------------------------------------------------------------------------
// Thèmes visuels par catégorie
// ---------------------------------------------------------------------------

export const STORE_THEME_BY_CATEGORY: Record<StoreCategoryId, StoreTheme> = {

  // ── Vêtements ─────────────────────────────────────────────────────────────
  CLOTHING_BRAND: {
    id: 'modern-elegant',
    name: 'Modern Elegant',
    vibe: 'Design moderne et élégant',
    categoryLabel: 'Marque de vêtements',
    background:       'linear-gradient(120deg, #f8fafc 0%, #ede9fe 42%, #fdf2f8 100%)',
    backgroundSoft:   '#f5f3ff',
    decorativeGlow:   'radial-gradient(circle at 15% 20%, rgba(99,102,241,0.26), transparent 48%)',
    heroOverlay:      'linear-gradient(120deg, rgba(30,27,75,0.85), rgba(79,70,229,0.74))',
    cardBackground:   'rgba(255,255,255,0.9)',
    text:             '#1f1147',
    mutedText:        '#5b4f84',
    primary:          '#4f46e5',
    primaryStrong:    '#3730a3',
    accent:           '#f472b6',
    headingFont:      'Playfair Display, serif',
    bodyFont:         'Manrope, sans-serif',
  },

  // ── Accessoires ───────────────────────────────────────────────────────────
  ACCESSORIES: {
    id: 'minimal-clean',
    name: 'Minimal Clean',
    vibe: 'Design minimaliste et épuré',
    categoryLabel: 'Accessoires',
    background:       'linear-gradient(115deg, #ffffff 0%, #f3f4f6 52%, #e5e7eb 100%)',
    backgroundSoft:   '#f3f4f6',
    decorativeGlow:   'radial-gradient(circle at 82% 12%, rgba(17,24,39,0.14), transparent 44%)',
    heroOverlay:      'linear-gradient(120deg, rgba(17,24,39,0.9), rgba(75,85,99,0.74))',
    cardBackground:   'rgba(255,255,255,0.95)',
    text:             '#111827',
    mutedText:        '#4b5563',
    primary:          '#111827',
    primaryStrong:    '#030712',
    accent:           '#9ca3af',
    headingFont:      'Sora, sans-serif',
    bodyFont:         'Inter, sans-serif',
  },

  // ── Art & Peintures ───────────────────────────────────────────────────────
  ART_PAINTINGS: {
    id: 'creative-colorful',
    name: 'Creative Colorful',
    vibe: 'Style créatif et coloré',
    categoryLabel: 'Tableaux & Art',
    background:       'linear-gradient(120deg, #fff7ed 0%, #fef3c7 28%, #dbeafe 64%, #fce7f3 100%)',
    backgroundSoft:   '#fff7ed',
    decorativeGlow:   'radial-gradient(circle at 20% 18%, rgba(244,63,94,0.3), transparent 50%)',
    heroOverlay:      'linear-gradient(120deg, rgba(194,24,91,0.8), rgba(59,130,246,0.76))',
    cardBackground:   'rgba(255,255,255,0.88)',
    text:             '#3f0f4f',
    mutedText:        '#6b257f',
    primary:          '#ec4899',
    primaryStrong:    '#be185d',
    accent:           '#3b82f6',
    headingFont:      'Abril Fatface, serif',
    bodyFont:         'Poppins, sans-serif',
  },

  // ── Sacs ──────────────────────────────────────────────────────────────────
  BAGS: {
    id: 'chic-fashion',
    name: 'Chic Fashion',
    vibe: 'Style chic et fashion',
    categoryLabel: 'Sacs',
    background:       'linear-gradient(120deg, #fff7ed 0%, #fef3c7 50%, #ffedd5 100%)',
    backgroundSoft:   '#fffbeb',
    decorativeGlow:   'radial-gradient(circle at 76% 15%, rgba(217,119,6,0.24), transparent 46%)',
    heroOverlay:      'linear-gradient(120deg, rgba(120,53,15,0.86), rgba(217,119,6,0.72))',
    cardBackground:   'rgba(255,251,235,0.9)',
    text:             '#422006',
    mutedText:        '#78350f',
    primary:          '#a16207',
    primaryStrong:    '#854d0e',
    accent:           '#f59e0b',
    headingFont:      'Cormorant Garamond, serif',
    bodyFont:         'Nunito Sans, sans-serif',
  },

  // ── Pâtisserie ────────────────────────────────────────────────────────────
  PASTRY: {
    id: 'sweet-bakery',
    name: 'Sweet Bakery',
    vibe: 'Couleurs douces et ambiance gourmande',
    categoryLabel: 'Pâtisserie (petits gâteaux & produits sains)',
    background:       'linear-gradient(120deg, #fff1f2 0%, #ffe4e6 35%, #fdf2f8 68%, #fef9c3 100%)',
    backgroundSoft:   '#fff1f2',
    decorativeGlow:   'radial-gradient(circle at 16% 14%, rgba(244,114,182,0.28), transparent 52%)',
    heroOverlay:      'linear-gradient(120deg, rgba(136,19,55,0.8), rgba(244,114,182,0.72))',
    cardBackground:   'rgba(255,255,255,0.9)',
    text:             '#5a1f33',
    mutedText:        '#9d174d',
    primary:          '#ec4899',
    primaryStrong:    '#be185d',
    accent:           '#f59e0b',
    headingFont:      'Pacifico, cursive',
    bodyFont:         'Quicksand, sans-serif',
  },

  // ── Vêtements de sport ────────────────────────────────────────────────────
  SPORTS_CLOTHING: {
    id: 'dynamic-energy',
    name: 'Dynamic Energy',
    vibe: 'Design dynamique et énergique',
    categoryLabel: 'Vêtements de sport',
    background:       'linear-gradient(120deg, #eff6ff 0%, #dbeafe 35%, #e0f2fe 64%, #dcfce7 100%)',
    backgroundSoft:   '#eff6ff',
    decorativeGlow:   'radial-gradient(circle at 82% 10%, rgba(14,165,233,0.3), transparent 46%)',
    heroOverlay:      'linear-gradient(120deg, rgba(3,105,161,0.86), rgba(16,185,129,0.74))',
    cardBackground:   'rgba(255,255,255,0.9)',
    text:             '#082f49',
    mutedText:        '#0f766e',
    primary:          '#0284c7',
    primaryStrong:    '#0369a1',
    accent:           '#10b981',
    headingFont:      'Bebas Neue, sans-serif',
    bodyFont:         'Montserrat, sans-serif',
  },

  // ── Cahiers personnalisés ─────────────────────────────────────────────────
  NOTEBOOKS: {
    id: 'paper-craft',
    name: 'Paper Craft',
    vibe: 'Ambiance papeterie artisanale et créative',
    categoryLabel: 'Cahiers personnalisés',
    background:       'linear-gradient(120deg, #fafaf7 0%, #f5f0e8 40%, #ede8dc 100%)',
    backgroundSoft:   '#f5f0e8',
    decorativeGlow:   'radial-gradient(circle at 22% 18%, rgba(180,140,90,0.22), transparent 50%)',
    heroOverlay:      'linear-gradient(120deg, rgba(62,39,18,0.88), rgba(139,90,43,0.76))',
    cardBackground:   'rgba(255,253,245,0.92)',
    text:             '#2c1a08',
    mutedText:        '#7a5c3a',
    primary:          '#8b5a2b',
    primaryStrong:    '#6b3f18',
    accent:           '#d4a853',
    headingFont:      'Libre Baskerville, serif',
    bodyFont:         'Lato, sans-serif',
  },

  // ── Tasses personnalisées ─────────────────────────────────────────────────
  MUGS: {
    id: 'cozy-morning',
    name: 'Cozy Morning',
    vibe: 'Chaleur matinale, confort et bonne humeur',
    categoryLabel: 'Tasses personnalisées',
    background:       'linear-gradient(120deg, #fdf6ec 0%, #fdebd0 38%, #fce4c8 70%, #fdf0e0 100%)',
    backgroundSoft:   '#fdf6ec',
    decorativeGlow:   'radial-gradient(circle at 78% 16%, rgba(230,126,34,0.28), transparent 48%)',
    heroOverlay:      'linear-gradient(120deg, rgba(100,40,10,0.85), rgba(230,126,34,0.72))',
    cardBackground:   'rgba(255,250,242,0.93)',
    text:             '#3d1a00',
    mutedText:        '#a0522d',
    primary:          '#e67e22',
    primaryStrong:    '#ca6f1e',
    accent:           '#f39c12',
    headingFont:      'Josefin Slab, serif',
    bodyFont:         'Nunito, sans-serif',
  },
};

// ---------------------------------------------------------------------------
// Fonctions utilitaires — Thème & Catégorie
// ---------------------------------------------------------------------------

/**
 * Retourne le thème visuel correspondant à une catégorie.
 * Retourne le thème par défaut (CLOTHING_BRAND) si la catégorie est inconnue.
 */
export function getStoreTheme(categoryId: StoreCategoryId | ''): StoreTheme {
  if (!categoryId) return STORE_THEME_BY_CATEGORY.CLOTHING_BRAND;
  return STORE_THEME_BY_CATEGORY[categoryId] ?? STORE_THEME_BY_CATEGORY.CLOTHING_BRAND;
}

/**
 * Retourne le label français d'une catégorie, ou une chaîne vide.
 */
export function getCategoryLabel(categoryId: StoreCategoryId | ''): string {
  if (!categoryId) return '';
  return STORE_CATEGORY_OPTIONS.find((opt) => opt.id === categoryId)?.label ?? '';
}

// ---------------------------------------------------------------------------
// Fonctions utilitaires — Profils publics (localStorage)
// ---------------------------------------------------------------------------

/** Normalise une valeur en clé de stockage (minuscule, sans espaces superflus). */
function toStorageKey(value: string | undefined | null): string {
  return String(value ?? '').trim().toLowerCase();
}

/** Charge le dictionnaire complet des profils depuis le localStorage. */
function loadProfiles(): Record<string, PublicStoreProfile> {
  try {
    return JSON.parse(localStorage.getItem(PUBLIC_STORE_PROFILES_KEY) ?? '{}');
  } catch {
    return {};
  }
}

/** Persiste le dictionnaire complet des profils dans le localStorage. */
function persistProfiles(profiles: Record<string, PublicStoreProfile>): void {
  localStorage.setItem(PUBLIC_STORE_PROFILES_KEY, JSON.stringify(profiles));
}

/**
 * Sauvegarde un profil de boutique publiquement.
 * Appelé lors de la création d'une boutique (étape 2).
 */
export function savePublicStoreProfile(profile: CreatedStoreProfile): void {
  try {
    const profiles = loadProfiles();
    const key = toStorageKey(profile.name);
    profiles[key] = { ...profile, id: key };
    persistProfiles(profiles);
  } catch (error) {
    console.error('[EspritMarket] savePublicStoreProfile :', error);
  }
}

/**
 * Recherche un profil public par nom de boutique, nom du propriétaire ou e-mail.
 * Retourne `null` si aucun résultat.
 */
export function getPublicStoreProfile(storeName: string): PublicStoreProfile | null {
  try {
    const profiles = loadProfiles();
    const key = toStorageKey(storeName);

    // Correspondance directe sur la clé
    if (profiles[key]) return profiles[key];

    // Recherche élargie (propriétaire / e-mail)
    return (
      Object.values(profiles).find(
        (p) =>
          toStorageKey(p.name)       === key ||
          toStorageKey(p.ownerName)  === key ||
          toStorageKey(p.ownerEmail) === key
      ) ?? null
    );
  } catch (error) {
    console.error('[EspritMarket] getPublicStoreProfile :', error);
    return null;
  }
}

/**
 * Retourne tous les profils publics (pour l'annuaire des boutiques).
 */
export function getAllPublicStoreProfiles(): PublicStoreProfile[] {
  try {
    return Object.values(loadProfiles());
  } catch (error) {
    console.error('[EspritMarket] getAllPublicStoreProfiles :', error);
    return [];
  }
}

/**
 * Supprime un profil public par nom de boutique, nom du propriétaire ou e-mail.
 */
export function deletePublicStoreProfile(storeName: string): void {
  try {
    const profiles = loadProfiles();
    const key = toStorageKey(storeName);

    // Suppression directe si la clé existe
    if (key in profiles) {
      delete profiles[key];
      persistProfiles(profiles);
      return;
    }

    // Recherche élargie avant suppression
    const matchedKey = Object.keys(profiles).find((k) => {
      const p = profiles[k];
      return (
        toStorageKey(p?.name)       === key ||
        toStorageKey(p?.ownerName)  === key ||
        toStorageKey(p?.ownerEmail) === key
      );
    });

    if (matchedKey) {
      delete profiles[matchedKey];
      persistProfiles(profiles);
    }
  } catch (error) {
    console.error('[EspritMarket] deletePublicStoreProfile :', error);
  }
}

/**
 * Met à jour partiellement un profil public existant.
 * Retourne `true` si la mise à jour a réussi, `false` sinon.
 */
export function updatePublicStoreProfile(
  storeName: string,
  updates: Partial<CreatedStoreProfile>
): boolean {
  try {
    const profiles = loadProfiles();
    const key = toStorageKey(storeName);

    const targetKey =
      key in profiles
        ? key
        : Object.keys(profiles).find((k) => {
            const p = profiles[k];
            return (
              toStorageKey(p?.name)       === key ||
              toStorageKey(p?.ownerName)  === key ||
              toStorageKey(p?.ownerEmail) === key
            );
          });

    if (!targetKey) return false;

    profiles[targetKey] = { ...profiles[targetKey], ...updates };
    persistProfiles(profiles);
    return true;
  } catch (error) {
    console.error('[EspritMarket] updatePublicStoreProfile :', error);
    return false;
  }
}