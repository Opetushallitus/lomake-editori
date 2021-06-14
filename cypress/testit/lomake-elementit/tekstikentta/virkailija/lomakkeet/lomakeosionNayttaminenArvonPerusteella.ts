export const lomakeosionNayttaminenArvonPerusteella = {
  lomakeosionNayttaminenArvonPerusteellaValinta: () =>
    cy.get(
      '[data-test-id=tekstikenttä-valinta-lomakeosion-nayttaminen-arvon-perusteella]'
    ),

  valitseLomakeosionNayttaminenArvonPerusteella: () => {
    return lomakeosionNayttaminenArvonPerusteella
      .lomakeosionNayttaminenArvonPerusteellaValinta()
      .click()
  },

  lomakeosionNayttamissaanto: () =>
    cy.get('[data-test-id=tekstikenttä-lomakeosion-näyttämissääntö]'),

  lomakeosionNayttamissaantoEhtoOperaattori: () =>
    lomakeosionNayttaminenArvonPerusteella
      .lomakeosionNayttamissaanto()
      .find(
        '[data-test-id=tekstikenttä-lisäkysymys-arvon-perusteella-ehto-operaattori]'
      ),

  lomakeosionNayttamissaantoEhtoVertailuarvo: () =>
    lomakeosionNayttaminenArvonPerusteella
      .lomakeosionNayttamissaanto()
      .find(
        '[data-test-id=tekstikenttä-lisäkysymys-arvon-perusteella-ehto-vertailuarvo]'
      ),

  lomakeosionNayttamissaantoLomakeOsionTeksti: () =>
    lomakeosionNayttaminenArvonPerusteella
      .lomakeosionNayttamissaanto()
      .find(
        '[data-test-id=tekstikenttä-arvon-perusteella-piilotettavan-osion-nimi]'
      ),

  haePiilotettavanLomakeosionTeksti: () => {
    return lomakeosionNayttaminenArvonPerusteella.lomakeosionNayttamissaantoLomakeOsionTeksti()
  },

  asetaLomakeosionNayttaminenArvonPerusteellaEhto: (
    operaattori: string,
    vertailuarvo: number
  ) => {
    lomakeosionNayttaminenArvonPerusteella
      .lomakeosionNayttamissaantoEhtoOperaattori()
      .select(operaattori)
    lomakeosionNayttaminenArvonPerusteella
      .lomakeosionNayttamissaantoEhtoVertailuarvo()
      .type(`${vertailuarvo}`)
  },
}
