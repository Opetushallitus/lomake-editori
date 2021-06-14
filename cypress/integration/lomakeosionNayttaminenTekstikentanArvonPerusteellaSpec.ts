import kirjautuminenVirkailijanNakymaan from '../testit/kirjautuminenVirkailijanNakymaan'
import lomakkeenLuonti from '../testit/lomakkeenLuonti'
import hakijanNakymaanSiirtyminen from '../testit/hakijanNakymaanSiirtyminen'
import * as hakijanNakyma from '../hakijanNakyma'
import lomakeosionNayttaminenTekstikentanArvonPerusteellaLisays from '../testit/lomake-elementit/tekstikentta/virkailija/lomakkeet/lomakeosionNayttaminenTekstikentanArvonPerusteellaLisays'
import * as tekstinSyotto from '../tekstinSyotto'

const typeValueAndAssertVisibility = (value: string, isVisible: boolean) => {
  tekstinSyotto
    .syotaTekstiTarkistamatta(
      cy.get('[data-test-id=tekstikenttä-input]'),
      value
    )
    .then(() => {
      hakijanNakyma.henkilotiedot
        .etunimi()
        .should(isVisible ? 'exist' : 'not.exist')
    })
}

describe('Lomakeosion näkyvyys tekstikentän arvon perusteella', () => {
  kirjautuminenVirkailijanNakymaan('lomakkeiden käsittelyä varten', () => {
    lomakkeenLuonti((lomakkeenTunnisteet) => {
      lomakeosionNayttaminenTekstikentanArvonPerusteellaLisays(
        lomakkeenTunnisteet,
        () => {
          hakijanNakymaanSiirtyminen(lomakkeenTunnisteet, () => {
            it('Ei näytä valittua lomakeosiota kun lomakkeen avaa ensimmäistä kertaa', () => {
              hakijanNakyma.henkilotiedot.etunimi().should('not.exist')
            })
            it('Piilottaa lomakeosion, kun tekstikentän ehto täyttyy', () => {
              typeValueAndAssertVisibility('-1', false)
            })
            it('Näyttää lomakeosion, kun tekstikentän ehto ei täyty', () => {
              typeValueAndAssertVisibility('0', true)
              typeValueAndAssertVisibility('10', true)
            })
            it('Piilottaa lomakeosion, kun tekstikenttä on tyhjä', () => {
              cy.get('[data-test-id=tekstikenttä-input]')
                .clear()
                .then(() => {
                  hakijanNakyma.henkilotiedot.etunimi().should('not.exist')
                })
            })
          })
        }
      )
    })
  })
})
