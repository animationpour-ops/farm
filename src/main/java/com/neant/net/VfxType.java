package com.neant.net;

/** Identifiants des effets visuels envoyes du serveur vers les clients. */
public enum VfxType {
    /** Foudre (reutilisee par la forge et la faille). */
    SMITE,
    /** Arc electrique entre deux points. */
    ARC,
    /** Trou noir (pelle, gardien). */
    BLACK_HOLE,
    /** Effondrement / onde de choc spherique. */
    COLLAPSE,
    /** Faille du Neant dans le ciel. */
    RIFT,
    /** Onde de coupe de l'epee. */
    SLASH,
    /** Faisceau de la foreuse (pioche). */
    DRILL,
    /** Tourbillon de lames (hache). */
    SPIN,
    /** Vague de flammes du Neant (briquet). */
    FIREWAVE,
    /** Implosion d'une zone de blocs effaces. */
    IMPLODE,
    /** Aura (armure complete, sbires de la faille). */
    AURA,
    /** Trainee de sprint. */
    TRAIL,
    /** Teleportation du vide. */
    BLINK,
    /** Pulsation du gardien. */
    PULSE,
    /** Rituel de la forge. */
    RITUAL,
    /** Supprime les effets d'un type donne attaches a une entite. */
    CANCEL;

    public static VfxType byId(int id) {
        VfxType[] all = values();
        return id >= 0 && id < all.length ? all[id] : CANCEL;
    }
}
