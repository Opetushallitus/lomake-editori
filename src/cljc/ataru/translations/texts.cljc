(ns ataru.translations.texts
  (:require [clojure.string :as string]))

(def translation-mapping
  {:lisaa-kieli                                 {:fi "Lisää kieli"
                                                 :sv "Lägg till språk"
                                                 :en "Lisää kieli"}
   :add                                         {:fi "Lisää"
                                                 :sv "Lägg till"
                                                 :en "Add"}
   :add-application-option                      {:fi "Lisää hakukohde"
                                                 :sv "Lägg till ansökningsmål"
                                                 :en "Add study programme"}
   :add-attachment                              {:fi "Lisää liite..."
                                                 :en "Upload attachment..."
                                                 :sv "Ladda upp bilagan..."}
   :add-more                                    {:fi "Lisää..."
                                                 :sv "Lägg till..."
                                                 :en "Add..."}
   :add-more-button                             {:fi "Lisää"
                                                 :sv "Lägg till"
                                                 :en "Add"}
   :add-row                                     {:fi "Lisää rivi"
                                                 :sv "Lägg till rad"
                                                 :en "Add row"}
   :allow-publishing-of-results-online          {:fi "Jos tulen hyväksytyksi, oppilaitos voi julkaista nimeni omilla verkkosivuillaan."
                                                 :sv "Om jag blir antagen, får läroanstalen publicera mitt namn på sin webbplats."
                                                 :en "Education institution may publish my admission results on their web page."}
   :allow-use-of-contact-information            {:fi "Annan suostumuksen yhteystietojeni luovuttamiseen koulutusta koskevaa suoramarkkinointia varten."
                                                 :sv "Mina kontaktuppgifter får överlåtas för direkt marknadsföring angående utbildning."
                                                 :en "My contact information can be given to third parties for the purpose of direct education marketing."}
   :application-can-be-found-here               {:fi "Hakemuksesi löytyy täältä"
                                                 :sv "Din ansökan kan hittas här"
                                                 :en "You can find your application here"}
   :application-confirmation                    {:fi "Saat vahvistuksen sähköpostiisi"
                                                 :sv "Du får en bekräftelse till din e-post"
                                                 :en "Confirmation email will be sent to the email address you've provided"}
   :application-edited-subject                  {:fi "Opintopolku - Hakemuksesi on päivitetty"
                                                 :sv "Studieinfo - Din ansökan har updaterats"
                                                 :en "Opintopolku - Your application has been received"}
   :application-edited-text                     {:fi "Hakemuksesi on päivitetty."
                                                 :en "Your application has been updated."
                                                 :sv "Din ansökan har uppdaterats."}
   :application-hakija-edit-text                {:fi "LÄHETÄ MUUTOKSET"
                                                 :sv "SKICKA FÖRÄNDRINGAR"
                                                 :en "SEND MODIFICATIONS"}
   :application-period                          {:fi "Hakuaika"
                                                 :sv "Ansökningstid"
                                                 :en "Application period"}
   :application-period-less-than-day-left       {:fi "Hakuaikaa jäljellä alle vuorokausi"
                                                 :sv "Av ansökningstiden återstår mindre än ett dygn"
                                                 :en "Less than a day left in the application period"}
   :application-period-less-than-hour-left      {:fi "Hakuaikaa jäljellä alle tunti"
                                                 :sv "Av ansökningstiden återstår mindre än en timme"
                                                 :en "Less than one hour left in the application period"}
   :application-period-less-than-45-min-left    {:fi "Hakuaikaa jäljellä alle 45 min"
                                                 :sv "Av ansökningstiden återstår mindre än 45 min."
                                                 :en "Less than 45 min left in the application period"}
   :application-period-less-than-30-min-left    {:fi "Hakuaikaa jäljellä alle 30 min"
                                                 :sv "Av ansökningstiden återstår mindre än 30 min."
                                                 :en "Less than 30 min left in the application period"}
   :application-period-less-than-15-min-left    {:fi "Hakuaikaa jäljellä alle 15 min"
                                                 :sv "Av ansökningstiden återstår mindre än 15 min."
                                                 :en "Less than 15 min left in the application period"}
   :application-period-expired                  {:fi "Hakuaika on päättynyt"
                                                 :sv "Ansökningstiden har utgått"
                                                 :en "Application period has ended"}
   :application-priorization-invalid            {:fi "Hakukohteet ovat väärässä ensisijaisuusjärjestyksessä"
                                                 :sv "Fel prioritetsordning för ansökningsmålen"
                                                 :en "Study programmes are in an invalid order of preference"}
   :application-limit-reached-in-hakukohderyhma {:fi "Et voi hakea tähän hakukohteeseen, koska olet jo hakemassa seuraaviin hakukohteisiin:"
                                                 :sv "Du kan inte söka till detta ansökningsmål, eftersom du redan söker till följande ansökningsmål:"
                                                 :en "You can't apply to this study programme because you are applying to the following study programme:"}
   :application-processed-cant-modify           {:fi "Tämä hakemus on käsitelty eikä ole enää muokattavissa"
                                                 :sv "Denna ansökan har behandlats och kan inte längre bearbetas"
                                                 :en "This application has been processed and can no longer be modified"}
   :application-received-subject                {:fi "Opintopolku - Hakemuksesi on vastaanotettu"
                                                 :sv "Studieinfo - Din ansökan har tagits emot"
                                                 :en "Opintopolku - Your application has been received"}
   :application-received-text                   {:fi "Hakemuksesi on vastaanotettu."
                                                 :en "Your application has been received."
                                                 :sv "Din ansökan har tagits emot."}
   :application-sending                         {:fi "Hakemusta lähetetään"
                                                 :sv "Ansökan skickas"
                                                 :en "The application is being sent"}
   :application-sent                            {:fi "Hakemus lähetetty"
                                                 :sv "Ansökan har skickats"
                                                 :en "The application has been sent"}
   :application-submitted                       {:fi "Hakemuksesi on vastaanotettu!"
                                                 :sv "Din ansökan har tagits emot!"
                                                 :en "Your application has been received!"}
   :application-submitted-ok                    {:fi "OK"
                                                 :sv "OK"
                                                 :en "OK"}
   :application-virkailija-edit-text            {:fi "TALLENNA MUUTOKSET"
                                                 :sv "SPARA FÖRÄNDRINGARNA"
                                                 :en "SAVE MODIFICATIONS"}
   :applications_at_most                        {:fi "Tässä haussa voit hakea %s hakukohteeseen"
                                                 :sv "I denna ansökan kan du söka till %s ansökningsmål"
                                                 :en "In this application you can apply to %s study programmes "}
   :arvosana                                    {:fi "Arvosana"
                                                 :sv "Arvosana"
                                                 :en "Arvosana"}
   :best-regards                                {:fi "Ystävällisin terveisin"
                                                 :sv "Med vänliga hälsningar"
                                                 :en "Best Regards"}
   :should-be-higher-priorization-than          {:fi ["Hakukohde " " tulee olla korkeammalla prioriteetillä kuin "]
                                                 :sv ["Ansökningsmålet " " bör ha en högre prioritet än "]
                                                 :en ["Study programme " " has to be in higher order of preference than "]}
   :check-answers                               {:fi ["Tarkista " " tietoa"]
                                                 :sv ["Kontrollera " " uppgifter"]
                                                 :en ["Check " " answers"]}
   :contact-language                            {:fi "Asiointikieli"
                                                 :sv "Ärendespråk"
                                                 :en "Contact language"}
   :contact-language-info                       {:fi "Valitse kieli, jolla haluat vastaanottaa opiskelijavalintaan liittyviä tietoja. Toiveesi otetaan huomioon mahdollisuuksien mukaan."
                                                 :sv "Välj det språk på vilket du vill få information om studerandeantagningen. Ditt önskemål tas i beaktande om möjligt."
                                                 :en "Choose the language in which you wish to receive information regarding the student selection. Your choice will be taken into consideration if possible."}
   :continuous-period                           {:fi "Jatkuva haku"
                                                 :sv "Kontinuerlig ansökningstid"
                                                 :en "Continuous application period"}
   :do-not-share-warning-text                   {:fi "Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta."
                                                 :en "Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application."
                                                 :sv "Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet, om du använder en offentlig dator."}
   :english                                     {:fi "Englanti"
                                                 :sv "Engelska"
                                                 :en "English"}
   :expired-secret-button                       {:fi "Tilaa uusi hakemuslinkki sähköpostiisi"
                                                 :en "Send a new application link to your email"
                                                 :sv "Beställ en ny ansökningslänk till din e-post"}
   :expired-secret-contact                      {:fi "Ongelmatilanteessa ota yhteys hakemaasi oppilaitokseen."
                                                 :en "If problems arise, please contact the educational organization to which you have applied."
                                                 :sv "Vid eventuella problemsituationer bör du kontakta den läroanstalt du söker till."}
   :expired-secret-heading                      {:fi "Tämä hakemuslinkki on vanhentunut"
                                                 :en "This application link has expired"
                                                 :sv "Denna ansökningslänk är föråldrad"}
   :expired-secret-paragraph                    {:fi "Turvallisuussyistä hakemuslinkki on voimassa yhden muokkauskerran tai enintään %d päivää."
                                                 :en "For security reasons the link is valid for one application update or a maximum of %d days."
                                                 :sv "Av säkerhetsskäl är ansökningslänken i kraft under en session eller i högst %d dagar."}
   :expired-secret-sent                         {:fi "Uusi linkki lähetetty!"
                                                 :en "The new link has been sent!"
                                                 :sv "Den nya länken har skickats!"}
   :email-info-text                             {:fi "Varmista, että antamasi sähköpostiosoite on kirjoitettu oikein ja se on henkilökohtainen. Lähetämme sinulle tärkeitä viestejä tähän sähköpostiosoitteeseen."
                                                 :en "Please ensure that the given email address is typed correctly and is your personal address. Important messages will be sent to this email address."
                                                 :sv "Försäkra dig om att du skrivit din e-postadress rätt och att det är din personliga e-post. Vi skickar viktiga meddelanden till denna e-postadress."}
   :feedback-disclaimer                         {:fi "Yhteystietojasi ei käytetä tai yhdistetä palautteen tietoihin."
                                                 :en "Your personal information is not sent or associated with the feedback given."
                                                 :sv "Dina kontaktuppgifter används inte och kopplas inte heller ihop med responsuppgifterna."}
   :feedback-header                             {:fi "Kerro vielä mitä pidit hakulomakkeesta"
                                                 :en "Care to take a moment to rate our application form?"
                                                 :sv "Berätta ännu vad du tyckte om ansökningsblanketten"}
   :feedback-ratings                            {:fi {1 "Huono"
                                                      2 "Välttävä"
                                                      3 "Tyydyttävä"
                                                      4 "Hyvä"
                                                      5 "Kiitettävä"}
                                                 :en {1 "Poor"
                                                      2 "Passable"
                                                      3 "OK"
                                                      4 "Good"
                                                      5 "Excellent"}
                                                 :sv {1 "Dålig"
                                                      2 "Försvarlig"
                                                      3 "Nöjaktig"
                                                      4 "Bra"
                                                      5 "Berömlig"}}
   :feedback-send                               {:fi "Lähetä palaute"
                                                 :en "Send feedback"
                                                 :sv "Skicka respons"}
   :feedback-text-placeholder                   {:fi "Anna halutessasi kommentteja hakulomakkeesta."
                                                 :en "Feel free to also share your comments regarding the application form."
                                                 :sv "Om du vill kan du ge kommentarer om ansökningsblanketten."}
   :feedback-thanks                             {:fi "Kiitos palautteestasi!"
                                                 :en "Thank you for your feedback!"
                                                 :sv "Tack för din respons!"}
   :file-size-info                              {:fi "Tiedoston maksimikoko on %s"
                                                 :en "Maximum file size is %s"
                                                 :sv "Den maximala filstorleken är %s"}
   :file-size-info-min                          {:fi "Tiedoston koko on 0"
                                                 :en "The file size is 0"
                                                 :sv "Filstorleken är 0"}
   :uploading                                   {:fi "Ladataan"
                                                 :sv "Laddar upp"
                                                 :en "Uploading"}
   :processing-file                             {:fi "Käsitellään"
                                                 :sv "Bearbetar"
                                                 :en "Processing"}
   :deadline-in                                 {:fi "Palautettava viimeistään"
                                                 :sv "Sista leveransdatum"
                                                 :en "Deadline in"}
   :file-type-forbidden                         {:fi "Tiedostomuoto ei ole sallittu"
                                                 :en "File type is invalid"
                                                 :sv "Filformatet är inte tillåtet"}
   :file-upload-failed                          {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                                 :en "File failed to upload, try again"
                                                 :sv "Filen kunde inte laddas, försök igen"}
   :file-upload-retransmit                      {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                                 :en "File failed to upload, try again"
                                                 :sv "Filen kunde inte laddas, försök igen"}
   :file-upload-error                           {:fi "Tiedostoa ei ladattu, yritä uudelleen"
                                                 :en "File failed to upload, try again"
                                                 :sv "Filen kunde inte laddas, försök igen"}
   :cancel-remove                               {:fi "Älä poista"
                                                 :sv "Radera inte"
                                                 :en "Don't remove"}
   :confirm-remove                              {:fi "Vahvista poisto"
                                                 :sv "Bekräfta raderingen"
                                                 :en "Confirm removal"}
   :cancel-upload                               {:fi "Keskeytä"
                                                 :sv "Avbryta"
                                                 :en "Cancel"}
   :cancel-cancel-upload                        {:fi "Älä keskeytä"
                                                 :sv "Avbryta inte"
                                                 :en "Don't cancel"}
   :confirm-cancel-upload                       {:fi "Vahvista keskeytys"
                                                 :sv "Bekräfta avbrytning"
                                                 :en "Confirm cancel"}
   :finnish                                     {:fi "Suomi"
                                                 :sv "Finska"
                                                 :en "Finnish"}
   :hakija-new-text                             {:fi "LÄHETÄ HAKEMUS"
                                                 :sv "SKICKA ANSÖKAN"
                                                 :en "SUBMIT APPLICATION"}
   :hello-text                                  {:fi "Hei"
                                                 :sv "Hej"
                                                 :en "Hi"}
   :hour                                        {:fi "tunti"
                                                 :en "hour"
                                                 :sv "timme"}
   :hours                                       {:fi "tuntia"
                                                 :en "hours"
                                                 :sv "timmar"}
   :insufficient-base-education                 {:fi "Ilmoitus riittämättömästä pohjakoulutuksesta"
                                                 :sv "Meddelande om otillräcklig grundutbildning"
                                                 :en "Insufficient educational background"}
   :liitepyynto-for-hakukohde                   {:fi "Liitepyyntö kuuluu hakukohteisiin:"
                                                 :en "Request for attachment is for study programmes:"
                                                 :sv "Begäran om bilagor berör ansökningsmål:"}
   :lupatiedot-info                             {:fi "Tarkista hakulomakkeelle täyttämäsi tiedot huolellisesti ennen hakulomakkeen lähettämistä. \n\nHakemuksella antamiasi tietoja käytetään opiskelijavalintaan. Näiden tietojen lisäksi opiskelijavalinnassa käytetään perusopetuksen, lukiokoulutuksen ja ammatillisen koulutuksen valtakunnalliseen KOSKI-tietovarantoon tallennettuja tietoja, Ylioppilastutkintolautakunnasta ja korkeakouluista saatavia tutkinto- ja arvosanatietoja sekä väestötietojärjestelmästä saatavia henkilötietoja.  Henkilötietojesi käsittely perustuu lakiin valtakunnallisista opinto- ja tutkintorekistereistä (884/2017).\n\nOpiskelijavalinnan jälkeen tietosi siirtyvät korkeakoululle, josta sait opiskelupaikan. Tietojasi voidaan lakiin perustuen luovuttaa myös muille viranomaisille sekä tutkimustarkoitukseen.\n\nTiedot säilytetään lain mukaan viisi vuotta, jonka jälkeen tiedot siirretään Kansallisarkiston päätöksen mukaan pysyvään säilytykseen. Opiskelupaikan vastaanottamistiedot säilytetään lain mukaan pysyvästi.\n\nSinulla on oikeus tarkastaa tietosi sekä vaatia tietojen oikaisemista tai käsittelyn rajoittamista. Sinulla on myös oikeus tehdä valitus tietosuojavaltuutetulle.\n \nLisätietoja: [Opintopolun tietosuojaseloste](https://opintopolku.fi/wp/tietosuojaseloste/opintopolun-opiskelijavalintarekisterin-tietosuojaseloste/).\n\nSaat vahvistusviestin vastaanotetusta hakulomakkeesta sähköpostiisi."
                                                 :sv "Kontrollera noggrant de uppgifter som du har angett i ansökningsblanketten innan du skickar ansökningsblanketten. \n\nDe uppgifter som du har gett i din ansökan används för att genomföra antagningen av studerande. Utöver dessa uppgifter används uppgifter som sparats i informationsresursen Koski, examens- och vitsordsuppgifter från Studentexamensnämnden och högskolorna samt personuppgifter från Befolkningsdatasystemet. Behandlingen av dina uppgifter bygger på lagen om nationella studie- och examensregister (884/2017).\n\nEfter att antagningen av studerande har gjorts, överförs dina uppgifter till den högskolan där du fick en studieplats. Dina uppgifter kan enligt lag också ges till andra myndigheter eller för forskning.\n\nUppgifterna sparas enligt lag i fem år, varefter de enligt Riksarkivets beslut bevaras permanent. Uppgifterna om mottagande av studieplats bevaras också permanent.\n\nDu har rätt att granska dina egna uppgifter och be att uppgifterna ändras eller att behandlingen av dem begränsas. Du har dessutom rätt att begära ändring hos dataombudsmannen. \n\nMer information: [Studieinfors dataskyddsbeskrivning] (https://studieinfo.fi/wp/dataskyddsbeskrivning/dataskyddsbeskrivning-for-studieinfos-antagningsregister/).\n\nDu får i din e-post ett bekräftelsemeddelande över att ansökningsblanketten har kommit fram."
                                                 :en "Please check all the information you have given in the application before you submit the application. \n\nThe information given on the application will be used for student admission and selection purposes. In addition to this information, data stored on the National Data Register for Basic Education, General Upper Secondary Education and Vocational Education, data received from the Finnish Matriculation Examination Board and data regarding degrees and grades received from higher education institutes as well as personal data received from the Finnish Population Information System will be used in student admission and selection. The processing of personal data is based on the Act on the National Registers of Education Records, Qualifications and Degrees (884/2017).\n\nAfter the student admission and selection process your data is transferred to the higher education institute to which you have received the right to study. Your data can be transferred to other officials as well as for research purposes.\nData in the data registry are kept for five years, starting from the decision of student admission. After this, the data will be transferred to permanent storage, according to the decision made by the National Archives of Finland. Data concerning receiving the right to study are kept forever.\n\nYou have the right to demand rectification of false or inaccurate data as well as the right to demand the limitation of processing the data. Additionally, you have the right to file a complaint to a data protection supervisor.\n\nAdditional information: [Studyinfo's register description](https://studyinfo.fi/wp2/en/register/).\n\nYou will receive a confirmation of your application to your email."}
   :minute                                      {:fi "minuutti"
                                                 :en "minute"
                                                 :sv "minut"}
   :minutes                                     {:fi "minuuttia"
                                                 :en "minutes"
                                                 :sv "minuter"}
   :missing-input                               {:fi "Puuttuva tieto"
                                                 :en "Missing information"
                                                 :sv "Uppgift som saknas"}
   :modifications-saved                         {:fi "Muutokset tallennettu"
                                                 :sv "Ändringarna har sparats"
                                                 :en "The modifications have been saved"}
   :modify-link-text                            {:fi "Ylläolevan linkin kautta voit katsella ja muokata hakemustasi."
                                                 :en "You can view and modify your application using the link above."
                                                 :sv "Du kan se och redigera din ansökan via länken ovan."}
   :muokkaa-hakukohteita                        {:fi "Muokkaa hakukohteita"
                                                 :sv "Bearbeta ansökningsmål"
                                                 :en "Modify your study programmes"}
   :no-hakukohde-search-hits                    {:fi "Ei hakutuloksia"
                                                 :en "No search results found"
                                                 :sv "Inga sökresultat"}
   :not-applicable-for-hakukohteet              {:fi "Huomaathan, että pohjakoulutuksesi perusteella et ole halukelpoinen seuraaviin hakukohteisiin. Tarkista hakukelpoisuusvaatimukset hakukohteen valintaperusteista."
                                                 :sv "Märk, att du på basis av din grundutbildning inte är behörig att söka till detta ansökningsmål. Kontrollera behörighetskraven för ansökan i ansökningsmålets antagningsgrunder."
                                                 :en "Please note that the education you have given does not provide eligibility for these study programmes. Please check the required eligibility from the study programme’s admission criteria."}
   :not-editable-application-period-ended       {:fi "Tämä hakutoive ei ole muokattavissa koska sen hakuaika on päättynyt."
                                                 :sv "Ansökningsmålet kan inte bearbetas eftersom ansökningstiden har utgått."
                                                 :en "You can't modify this study programme because the application period has ended."}
   :not-selectable-application-period-ended     {:fi "Hakuaika ei ole käynnissä"
                                                 :sv "Ingen pågående ansökningstid"
                                                 :en "Application period not ongoing"}
   :not-within-application-period               {:fi "hakuaika ei ole käynnissä"
                                                 :sv "ingen pågående ansökningstid"
                                                 :en "application period currently not ongoing"}
   :oppiaine                                    {:fi "Oppiaine"
                                                 :sv "Läroämne"
                                                 :en "Oppiaine"}
   :valinnaisaine                               {:fi "Valinnaisaine"
                                                 :sv "Valfritt ämne"
                                                 :en "Valinnaisaine"}
   :oppimaara                                   {:fi "Oppimäärä"
                                                 :sv "Lärokurs"
                                                 :en "Oppimäärä"}
   :suomi-aidinkielena                          {:fi "Suomi äidinkielenä"
                                                 :sv "Finska som modersmål"
                                                 :en "Suomi äidinkielenä"}
   :suomi-toisena-kielena                       {:fi "Suomi toisena kielena"
                                                 :sv "Finska som andra språk"
                                                 :en "Suomi toisena kielena"}
   :suomi-viittomakielisille                    {:fi "Suomi viittomakielisille"
                                                 :sv "Finska för teckenspråkiga"
                                                 :en "Suomi viittomakielisille"}
   :suomi-saamenkielisille                      {:fi "Suomi saamenkielisille"
                                                 :sv "Finska för samiskspråkiga"
                                                 :en "Suomi saamenkielisille"}
   :ruotsi-aidinkielena                         {:fi "Ruotsi äidinkielenä"
                                                 :sv "Svenska som modersmål"
                                                 :en "Ruotsi äidinkielenä"}
   :ruotsi-toisena-kielena                      {:fi "Ruotsi toisena kielenä"
                                                 :sv "Svenska som andra språk"
                                                 :en "Ruotsi toisena kielenä"}
   :ruotsi-viittomakielisille                   {:fi "Ruotsi viittomakielisille"
                                                 :sv "Svenska för teckenspråkiga"
                                                 :en "Ruotsi viittomakielisille"}
   :saame-aidinkielena                          {:fi "Saame äidinkielenä"
                                                 :sv "Samiska som modersmål"
                                                 :en "Saame äidinkielenä"}
   :romani-aidinkielena                         {:fi "Romani äidinkielenä"
                                                 :sv "Romani som modersmål"
                                                 :en "Romani äidinkielenä"}
   :viittomakieli-aidinkielena                  {:fi "Viittomakieli äidinkielenä"
                                                 :sv "Teckenspråk som modersmål"
                                                 :en "Viittomakieli äidinkielenä"}
   :muu-oppilaan-aidinkieli                     {:fi "Muu oppilaan äidinkieli"
                                                 :sv "Annat modersmål för eleven"
                                                 :en "Muu oppilaan äidinkieli"}
   :page-title                                  {:fi "Opintopolku – hakulomake"
                                                 :en "Studyinfo – application form"
                                                 :sv "Studieinfo – ansökningsblankett"}
   :permission-for-electronic-transactions      {:fi "Lupa sähköiseen asiointiin"
                                                 :sv "Medgivande till elektronisk kommunikation"
                                                 :en "Consent for electronic communication."}
   :permission-for-electronic-transactions-info {:fi "Täyttämällä sähköisen hakulomakkeen annat samalla luvan siihen, että opiskelijavalintaan liittyvä viestintä voidaan hoitaa pelkästään sähköisesti. Jos et suostu näihin ehtoihin, ota yhteyttä ensisijaisen hakutoiveesi korkeakoulun hakijapalveluihin."
                                                 :sv "Genom att fylla i denna elektroniska ansökningsblankett ger du samtidigt ditt medgivande till att kommunikationen gällande studerandeantagningen kan skötas enbart elektroniskt. Om du inte går med på dessa villkor, kontakta ansökningsservicen vid högskolan."
                                                 :en "By filling in this electronic application form you also give your consent that communication regarding student admissions can be carried out only by email. If you do not agree to these terms, please contact the admissions services of the higher education institution that you are applying to."}
   :permissions                                 {:fi "Lupatiedot"
                                                 :sv "Tilläggsuppgifter"
                                                 :en "Permissions"}
   :pohjakoulutusvaatimus                       {:fi "Pohjakoulutusvaatimus"
                                                 :sv "Grundutbildningskrav"
                                                 :en "Educational background restrictions"}
   :preview                                     {:fi "Esikatselu"
                                                 :en "Preview"
                                                 :sv "Förhandsvisa"}
   :question-for-hakukohde                      {:fi "Kysymys kuuluu hakukohteisiin:"
                                                 :en "This question is for study programmes:"
                                                 :sv "Frågan berör ansökningsmål:"}
   :info-for-hakukohde                          {:fi "Osio kuuluu hakukohteisiin:"
                                                 :en "This information is shown for study programmes:"
                                                 :sv "Frågan berör ansökningsmål:"}
   :show-application-options                    {:fi "Näytä hakukohteet"
                                                 :en "Show study programmes"
                                                 :sv "Visa ansökningsmål"}
   :hide-application-options                    {:fi "Piilota hakukohteet"
                                                 :en "Hide study programmes"
                                                 :sv "Dölj ansökningsmål"}
   :limit-reached                               {:fi "ei valittavissa"
                                                 :en "unselectable"
                                                 :sv "icke-valbar"}
   :read-less                                   {:fi "Sulje ohje"
                                                 :sv "Dölj anvisning"
                                                 :en "Hide instructions"}
   :read-more                                   {:fi "Lue lisää"
                                                 :sv "Läs mer"
                                                 :en "Read more"}
   :remove                                      {:fi "Poista"
                                                 :sv "Ta bort"
                                                 :en "Remove"}
   :remove-row                                  {:fi "Poista rivi"
                                                 :sv "Ta bort rad"
                                                 :en "Remove row"}
   :search-application-options                  {:fi "Etsi tämän haun koulutuksia"
                                                 :sv "Hämta ansökningsmål i denna ansökan"
                                                 :en "Search for study programmes"}
   :second                                      {:fi "sekunti"
                                                 :en "second"
                                                 :sv "sekund"}
   :seconds                                     {:fi "sekuntia"
                                                 :en "seconds"
                                                 :sv "sekunder"}
   :show-more                                   {:fi "Näytä lisää..."
                                                 :en "Show more..."
                                                 :sv "Visa mer..."}
   :swedish                                     {:fi "Ruotsi"
                                                 :sv "Svenska"
                                                 :en "Swedish"}
   :verify-email                                {:fi "Kirjoita sähköpostiosoitteesi uudelleen"
                                                 :sv "Ange din e-postadress igen"
                                                 :en "Please write your e-mail address again"}
   :window-close-warning                        {:fi "Varmistathan että hakemus on lähetetty ennen sivun sulkemista."
                                                 :en "Please ensure you have submitted the form before closing the page."
                                                 :sv "Försäkra dig om att du har skickat din ansökan innan du stänger sidan"}
   :edit-answers                                {:fi "Muokkaus"
                                                 :sv "Bearbeta"
                                                 :en "Edit"}
   :hyvaksytty                                  {:fi "Hyväksytty"
                                                 :sv "Godkänd"
                                                 :en "Selected"}
   :hyvaksytty-s                                {:fi "S (Hyväksytty)"
                                                 :sv "S (Godkänd)"
                                                 :en "S (Selected)"}
   :osallistunut-o                              {:fi "O (Osallistunut)"
                                                 :sv "O (Deltagit)"
                                                 :en "O (Osallistunut)"}
   :ei-arvosanaa                                {:fi "Ei arvosanaa"
                                                 :sv "Inget vitsord"
                                                 :en "Ei arvosanaa"}
   :preview-answers                             {:fi "Esikatselu"
                                                 :sv "Förhandsvisa"
                                                 :en "Preview"}
   :network-offline                             {:fi "Ei yhteyttä verkkoon."
                                                 :sv "Network is offline."
                                                 :en "Network is offline."}
   :application-period-closed                   {:fi "Hakemusta ei voi lähettää: Hakuaika on päättynyt tai liitteen määräaika on umpeutunut"
                                                 :sv "Ansökan kan inte skickas: Ansökningstiden eller deadline för bilagor har utgått"
                                                 :en "Application cannot be submitted: Application period has ended or deadline for submitting attachments has passed"}
   :inactivated                                 {:fi "Hakemuksesi on peruttu."
                                                 :sv "Din ansökning har annullerats."
                                                 :en "Your application is cancelled."}
   :internal-server-error                       {:fi "Tapahtui palvelinvirhe."
                                                 :sv "Internal server error."
                                                 :en "Internal server error."}
   :application-validation-failed-error         {:fi "Tapahtui palvelinvirhe."
                                                 :sv "Internal server error."
                                                 :en "Internal server error."}
   :secret-expired                              {:fi "Tämä hakemuslinkki on vanhentunut"
                                                 :en "This application link has expired"
                                                 :sv "Denna ansökningslänk är föråldrad"}
   :oppiaine-valinnainen                        {:fi "%s, valinnainen"
                                                 :sv "%s, valinnainen"
                                                 :en "%s, valinnainen"}
   :lisaa-valinnaisaine                         {:fi "Lisää valinnaisaine"
                                                 :sv "Lägg till valfritt ämne"
                                                 :en "Lisää valinnaisaine"}
   :poista                                      {:fi "Poista"
                                                 :sv "Radera"
                                                 :en "Poista"}})

(def oppiaine-translations
  {:oppiaine-a {:fi "Äidinkieli ja kirjallisuus"
                :sv "Modersmål och litteratur"
                :en "Äidinkieli ja kirjallisuus"}
   :oppiaine-a1 {:fi "A1-kieli"
                 :sv "A1-språk"
                 :en "A1-kieli"}
   :oppiaine-a2 {:fi "A2-kieli"
                 :sv "A2-språk"
                 :en "A2-kieli"}
   :oppiaine-b1 {:fi "B1-kieli"
                 :sv "B1-språk"
                 :en "B1-kieli"}
   :oppiaine-b2 {:fi "B2-kieli"
                 :sv "B2-språk"
                 :en "B2-kieli"}
   :oppiaine-b3 {:fi "B3-kieli"
                 :sv "B3-språk"
                 :en "B3-kieli"}
   :oppiaine-ma {:fi "Matematiikka"
                 :sv "Matematik"
                 :en "Matematiikka"}
   :oppiaine-bi {:fi "Biologia"
                 :sv "Biologi"
                 :en "Biologia"}
   :oppiaine-ge {:fi "Maantieto"
                 :sv "Geografi"
                 :en "Maantieto"}
   :oppiaine-fy {:fi "Fysiikka"
                 :sv "Fysik"
                 :en "Fysiikka"}
   :oppiaine-ke {:fi "Kemia"
                 :sv "Kemi"
                 :en "Kemia"}
   :oppiaine-tt {:fi "Terveystieto"
                 :sv "Hälsokunskap"
                 :en "Terveystieto"}
   :oppiaine-ty {:fi "Uskonto tai elämänkatsomustieto"
                 :sv "Religion eller livsåskådningskunskap"
                 :en "Uskonto tai elämänkatsomustieto"}
   :oppiaine-hi {:fi "Historia"
                 :sv "Historia"
                 :en "Historia"}
   :oppiaine-yh {:fi "Yhteiskuntaoppi"
                 :sv "Samhällslära"
                 :en "Yhteiskuntaoppi"}
   :oppiaine-mu {:fi "Musiikki"
                 :sv "Musik"
                 :en "Musiikki"}
   :oppiaine-ku {:fi "Kuvaamataito"
                 :sv "Bildkonst"
                 :en "Kuvaamataito"}
   :oppiaine-ka {:fi "Käsityö"
                 :sv "Slöjd"
                 :en "Käsityö"}
   :oppiaine-li {:fi "Liikunta"
                 :sv "Gymnastik"
                 :en "Liikunta"}
   :oppiaine-ko {:fi "Kotitalous"
                 :sv "Huslig ekonomi"
                 :en "Kotitalous"}
   :oppiaine-valinnainen-kieli {:fi "Valinnainen kieli"
                                :sv "Valfritt språk"
                                :en "Valinnainen kieli"}})

(def general-texts
  {:yes                {:en "Yes"
                        :fi "Kyllä"
                        :sv "Ja"}
   :no                 {:en "No"
                        :fi "Ei"
                        :sv "Nej"}
   :have-not           {:en "No"
                        :fi "En"
                        :sv "Nej"}
   :year-of-completion {:en "Year of completion"
                        :fi "Suoritusvuosi"
                        :sv "Avlagd år"}})

(def base-education-module-texts
  {:title                            {:fi "Koulutustausta"
                                      :sv "Utbildningsbakgrund"
                                      :en "Eligibility"}
   :completed-education              {:en "Fill in the education that you have completed  or will complete during the application term."
                                      :fi "Merkitse suorittamasi pohjakoulutukset, myös ne jotka suoritat hakukautena."
                                      :sv "Ange avlagda grundutbildningar, samt de som du avlägger under ansökningsperioden"}
   :higher-education-qualification   {:en "Higher education qualification completed in Finland"
                                      :fi "Suomessa suoritettu korkeakoulututkinto "
                                      :sv "Högskoleexamen som avlagts i Finland"}
   :qualification-level              {:en "Qualification level"
                                      :fi "Tutkintotaso"
                                      :sv "Examensnivå"}
   :completion-year-and-date         {:en "Year and date of completion"
                                      :fi "Suoritusvuosi ja päivämäärä"
                                      :sv "År och datum då examen avlagts"}
   :qualification                    {:en "Qualification/degree"
                                      :fi "Tutkinto"
                                      :sv "Examen"}
   :studies-required                 {:en "Studies required by the higher education institution completed at open university or open university of applied sciences (UAS)"
                                      :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot "
                                      :sv "Studier som högskolan kräver vid en öppen högskola"}
   :field                            {:en "Field"
                                      :fi "Ala"
                                      :sv "Bransch"}
   :module                           {:en "Study module"
                                      :fi "Opintokokonaisuus "
                                      :sv "Studiehelhet"}
   :scope                            {:en "Scope"
                                      :fi "Laajuus "
                                      :sv "Omfattning"}
   :institution                      {:en "Higher education institution"
                                      :fi "Korkeakoulu"
                                      :sv "Högskola"}
   :higher-education-outside-finland {:en "Higher education qualification completed outside Finland"
                                      :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto "
                                      :sv "Högskoleexamen som avlagts annanstans än i Finland"}
   :qualification-country            {:en "Country where the qualification has been awarded"
                                      :fi "Suoritusmaa"
                                      :sv "Land där examen har avlagts"}
   :other-eligibility                {:en "Other eligibility for higher education"
                                      :fi "Muu korkeakoulukelpoisuus"
                                      :sv "Övrig högskolebehörighet"}
   :describe-eligibility             {:en "Describe eligibility"
                                      :fi "Kelpoisuuden kuvaus"
                                      :sv "Beskrivning av behörigheten"}
   :have-you-completed               {:en "Have you completed general upper secondary education or vocational qualification?"
                                      :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon?"
                                      :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
   :choose-country                   {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification, but are in the process of doing so, please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification, school-level qualification, post-secondary level qualification, higher vocational level qualification, further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification."
                                      :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista, valitse se maa, jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto, ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata."
                                      :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det, välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå, yrkesinriktad examen på högre nivå, yrkesexamen eller specialyrkesexamen. Ange inte det land där du avlagt högskoleexamen."}})

(def higher-base-education-module-texts
  {:educational-background                                 {:en "Your educational background" :fi "Koodistopohjainen pohjakoulutusosio" :sv "Utbildningsbakgrund"}
   :completed-education                                    {:en "Fill in the education that you have completed  or will complete during the admission process (autumn 2018)"
                                                            :fi "Ilmoita kaikki suorittamasi koulutukset. Myös ne jotka suoritat hakukautena (syksy 2018)."
                                                            :sv "Ange alla utbildningar som du har avlagt. Ange också dem som du avlägger under ansökningsperioden (hösten 2018)."}
   :matriculation-exam-in-finland                          {:en "Matriculation examination completed in Finland"
                                                            :fi "Suomessa suoritettu ylioppilastutkinto"
                                                            :sv "Studentexamen som avlagts i Finland"}
   :marticulation-exam                                     {:en "Matriculation examination"
                                                            :fi "Ylioppilastutkinto"
                                                            :sv "Studentexamen"}
   :completed-marticaulation-before-1990?                  {:en "Have you completed your Matriculation examination in Finland in 1990 or after?"
                                                            :fi "Oletko suorittanut ylioppilastutkinnon vuonna 1990 tai sen jälkeen?"
                                                            :sv "Har du avlagt studentexamen år 1990 eller senare?"}
   :marticaulation-before-1990                             {:en "Matriculation examination (completed before 1990)"
                                                            :fi "Ylioppilastutkinto (ennen vuotta 1990)"
                                                            :sv "Studentexamen (före år 1990)"}
   :year-of-completion                                     {:en "Year of completion"
                                                            :fi "Suoritusvuosi"
                                                            :sv "Avlagd år"}
   :automatic-marticulation-info                           {:en "Your matriculation examination details are received automatically from the Matriculation Examination Board."
                                                            :fi "Saamme ylioppilastutkintosi tiedot rekisteristämme."
                                                            :sv "Vi får uppgifterna om din studentexamen ur vårt register."}
   :automatic-qualification-info                           {:en "Your qualification details will be received automatically from Koski-register."
                                                            :fi "Saamme tutkintosi rekisteristämme."
                                                            :sv "Vi får uppgifterna om din examen ur vårt register."}
   :automatic-higher-qualification-info                    {:en "We will receive your degree details automatically."
                                                            :fi "Saamme korkeakoulututkintosi tiedot rekisteristämme."
                                                            :sv "Vi får uppgifterna om din högskoleexamen ur vårt register."}
   :submit-your-attachments                                {:en "Submit your attachments in pdf/jpg/png -format. If you cannot submit your attachments online please contact the higher education institution in question directly. The attachments have to be submitted or returned by 26 Sept at 3 pm Finnish time at the latest."
                                                            :fi "Tallenna liitteesi PDF/JPG/PNG-muodossa. Jos et voi tallentaa liitettä sähköisessä muodossa niin ota yhteyttä hakemaasi korkeakouluun joka pyytää liitettä. Liite tulee olla tallennettuna tai palautettuna viimeistään 26.9.2018 klo 15.00."
                                                            :sv "Spara dina bilagor i PDF/JPG/PNG form. Om du inte kan spara bilagorna i elektronisk forma, ska du kontakta den högskola som du har sökt till, vilken begär bilagor. Bilagorna ska vara sparade eller returnerade senast 26.9.2018 kl. 15.00."}
   :general-upper-secondary-school                         {:en "General upper secondary school syllabus completed in Finland (without matriculation examination)"
                                                            :fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"
                                                            :sv "Gymnasiets lärokurs som avlagts i Finland utan studentexamen"}
   :secondary-school-year-of-completion                    {:en "Year of the completion of general upper secondary school syllabus"
                                                            :fi "Lukion oppimäärän suoritusvuosi"
                                                            :sv "Gymnasiet lärokurs avlagd år"}
   :fill-year-of-completion                                {:en "Please fill in/choose the year of completion"
                                                            :fi "Merkitse vuosi jolloin sait lukion päättötodistuksen."
                                                            :sv "Ange det år då du fick gymnasiets avgångsbetyg. "}
   :educational-institution                                {:en "Educational institution" :fi "Oppilaitos" :sv "Läroanstalt"}
   :upper-secondary-school-attachment                      {:en "Upper secondary school certificate"
                                                            :fi "Lukion päättötodistus"
                                                            :sv "Gymnasiets avgångsbetyg"}
   :international-marticulation-exam                       {:en "International matriculation examination completed in Finland"
                                                            :fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                            :sv "Internationell studentexamen som avlagts i Finland"}
   :international-baccalaureate                            {:en "International Baccalaureate -diploma"
                                                            :fi "International Baccalaureate"
                                                            :sv "International Baccalaureate -examen"}
   :european-baccalaureate                                 {:en "European Baccalaureate -diploma"
                                                            :fi " Eurooppalainen ylioppilastutkinto"
                                                            :sv "European Baccalaureate -examen"}
   :reifeprufung                                           {:en "Reifeprüfung - diploma/ Deutsche Internationale Abiturprüfung"
                                                            :fi "Reifeprüfung"
                                                            :sv "Reifeprüfung - examen"}
   :request-attachment-international-exam                  {:en "Request for attachment on international examination"
                                                            :fi "Kansainvälisen ylioppilastutkinnon liitepyyntö"
                                                            :sv "Begäran om bilagor för internationell studentexamen"}
   :double-degree                                          {:en "Double degree (secondary level)"
                                                            :fi "Ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"
                                                            :sv "Yrkesinriktad grundexamen och studentexamen (dubbelexamen"}
   :double-degree-vocational-attachment                    {:en "Request for attachment on vocational qualification"
                                                            :fi "Kaksoistutkinnon liitepyyntö (ammatillinen tutkinto)"
                                                            :sv "Begäran om bilagor för dubbelexamen (yrkesinriktad examen)"}
   :double-degree-marticulation-attachment                 {:en "Request for attachment on Double degree/ matriculation examination before 1990."
                                                            :fi "Kaksoistutkinnon liitepyyntö (ylioppilastutkinto ennen vuotta 1990)"
                                                            :sv "Begäran om bilagor för dubbelexamen (studentexamen före år 1990)"}
   :marticulation-completion-year                          {:en "The year of completion of Matriculation examination"
                                                            :fi "Ylioppilastutkinnon suoritusvuosi"
                                                            :sv "Studentexamen avlagd år"}
   :vocational-completion-year                             {:en "The year of completion of vocational qualification"
                                                            :fi "Ammatillisen tutkinnon suoritusvuosi"
                                                            :sv "Yrkesinriktad examen avlagd år"}
   :vocational-qualification                               {:en "Vocational qualification"
                                                            :fi "Ammatillinen tutkinto"
                                                            :sv "Yrkesinriktad examen"}
   :scope-of-qualification                                 {:en "Scope of qualification"
                                                            :fi "Tutkinnon laajuus"
                                                            :sv "Examens omfattning"}
   :scope                                                  {:en "Scope of qualification"
                                                            :fi "Laajuus"
                                                            :sv "Omfattning"}
   :courses                                                {:en "Courses" :fi "Kurssia" :sv "Kurser"}
   :ects-credits                                           {:en "ECTS credits" :fi "Opintopistettä" :sv "Studiepoäng"}
   :study-weeks                                            {:en "Study weeks" :fi "Opintoviikkoa" :sv "Studieveckor"}
   :competence-points                                      {:en "Competence points" :fi "Osaamispistettä" :sv "Kompetenspoäng"}
   :hours                                                  {:en "Hours" :fi "Tuntia" :sv "Timmar"}
   :weekly-lessons                                         {:en "Weekly lessons per year"
                                                            :fi "Vuosiviikkotuntia"
                                                            :sv "Årsveckotimmar"}
   :years                                                  {:en "Years" :fi "Vuotta" :sv "År"}
   :finnish-vocational                                     {:en "Vocational upper secondary qualification, school-level qualification, post-secondary level qualification or higher vocational level qualification completed in Finland"
                                                            :fi "Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                                                            :sv "Yrkesinriktad grundexamen, examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"}
   :finnish-vocational-2017-or-after                       {:en "Have you completed the qualification in 2017 or after?"
                                                            :fi "Oletko suorittanut tutkinnon vuonna 2017 tai sen jälkeen?"
                                                            :sv "Har du avlagt examen år 2017 eller senare?"}
   :qualification                                          {:en "Qualification"
                                                            :fi "Tutkinto"
                                                            :sv "Examen"}
   :finnish-vocational-completed                           {:en "Have you completed your qualification as a competence based qualification in its entiretity?"
                                                            :fi "Oletko suorittanut tutkinnon kokonaan näyttötutkintona?"
                                                            :sv "Har du avlagt examen som fristående yrkesexamen?"}
   :finnish-vocational-attachment                          {:en "Request for attachment on vocational qualification."
                                                            :fi "Ammatillinen perustutkinto kouluasteen opistoasteen tai ammatillisen korkea-asteen tutkinnon liitepyyntö"
                                                            :sv "Begäran om bilagor för yrkesinriktad grundexamen, examen på skolnivå, examen på institutnivå eller examen på yrkesinriktad högre nivå"}
   :click-to-add-more                                      {:en "Click ADD if you want add further qualifications."
                                                            :fi "Paina LISÄÄ jos haluat lisätä useampia tutkintoja."
                                                            :sv "Tryck på LÄGG TILL om du vill lägga till flera examina."}
   :click-to-add-more-qualifications                       {:en "Click ADD if you want add further qualifications."
                                                            :fi "Paina LISÄÄ jos haluat lisätä useampia opintokokonaisuuksia."
                                                            :sv "Tryck på LÄGG TILL om du vill lägga till flera studiehelheter."}
   :finnish-vocational-or-special                          {:en "Further vocational qualification or specialist vocational qualification completed in Finland"
                                                            :fi "Suomessa suoritettu ammatti- tai erikoisammattitutkinto"
                                                            :sv "Yrkesexamen eller specialyrkesexamen som avlagts i Finland"}
   :finnish-special-before-2018                            {:en "Have you completed you Further vocational or Specialist vocational qualification in 2018?"
                                                            :fi "Oletko suorittanut ammatti- tai erikoisammattitutkinnon vuonna 2018?"
                                                            :sv "Har du avlagt en yrkesexamen eller en specialyrkesexamen år 2018?"}
   :finnish-special-attachment                             {:en "Vocational or specialist vocational qualification"
                                                            :fi "Ammatti- tai erikoisammattitutkinto"
                                                            :sv "En yrkesexamen eller en specialyrkesexamen"}
   :finnish-higher-education                               {:en "Higher education qualification completed in Finland"
                                                            :fi "Suomessa suoritettu korkeakoulututkinto"
                                                            :sv "Högskoleexamen som avlagts i Finland"}
   :finnish-higher-education-1995-or-after                 {:en "Have you compeleted your university or university of applied sciences degree in 1995 or after?"
                                                            :fi "Oletko suorittanut korkeakoulututkintosi vuonna 1995 tai sen jälkeen? "
                                                            :sv "Har du avlagt din högskoleexamen år 1995 eller senare? *"}
   :finnish-higher-education-degree-level                  {:en "Degree level"
                                                            :fi "Tutkintotaso"
                                                            :sv "Examensnivå"}
   :degree                                                 {:en "Degree"
                                                            :fi "Tutkinto"
                                                            :sv "Examen"}
   :higher-education-institution                           {:en "Higher education institution"
                                                            :fi "Korkeakoulu"
                                                            :sv "Högskola"}
   :higher-education-degree                                {:en "Higher education degree"
                                                            :fi "Korkeakoulututkinto"
                                                            :sv "Högskoleexamen"}
   :international-marticulation-outside-finland            {:en "International matriculation examination completed outside Finland"
                                                            :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                            :sv "Internationell studentexamen som avlagts annanstans än i Finland"}
   :international-marticulation-outside-finland-name       {:en "Name of examination/diploma"
                                                            :fi "Ylioppilastutkinto"
                                                            :sv "Studentexamen"}
   :international-marticulation-outside-finland-attachment {:en "International matriculation examination completed outside Finland"
                                                            :fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                            :sv "Internationell studentexamen som avlagts annanstans än i Finland"}
   :country-of-completion                                  {:en "Country of completion"
                                                            :fi "Suoritusmaa"
                                                            :sv "Land där examen är avlagd"}
   :higher-education-outside-finland                       {:en "Higher education qualification completed outside Finland"
                                                            :fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto"
                                                            :sv "Högskoleexamen som avlagts annanstans än i Finland"}
   :level-of-degree                                        {:en "Level of degree" :fi "Tutkintotaso" :sv "Examensnivå"}
   :year-and-date-of-completion                            {:en "Year and date of completion (DD.MM.YYYY)"
                                                            :fi "Suorituspäivämäärä - ja vuosi (pp.kk.vvvv)"
                                                            :sv "År och datum då examen avlagts (dd.mm.åååå)"}
   :other-qualification-foreign                            {:en "Other qualification completed outside Finland that provides eligibility to apply for higher education in the country in question"
                                                            :fi "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                            :sv "Övrig examen som avlagts annanstans än i Finland, som ger behörighet för högskolestudier i ifrågavarande land"}
   :other-qualification-foreign-attachment                 {:en "Request for attachment on education that provides eligibility for higher education in the awarding country."
                                                            :fi "Liitepyyntö muualla kuin Suomessa suoritetusta muusta tutkinnosta joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                            :sv "Begäran om bilagor för examen som avlagts annanstans än i Finland och som i landet ifråga ger ansökningsbehörighet för högskola. "}
   :base-education-open                                    {:en "Studies required by the higher education institution completed at open university or open polytechnic/UAS"
                                                            :fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"
                                                            :sv "Studier som högskolan kräver vid en öppen högskola"}
   :base-education-open-studies                            {:en "Open university/university of applied sciences studies"
                                                            :fi "Avoimen korkeakoulun opinnot"
                                                            :sv "Studier inom den öppna högskolan"}
   :base-education-open-attachment                         {:en "Request for attachment on open university/university of applied sciences studies"
                                                            :fi "Avoimen korkeakouluopintojen liitepyyntö"
                                                            :sv "Begäran om bilagor för studier inom den öppna högskolan"}
   :field                                                  {:en "Study field" :fi "Ala" :sv "Bransch"}
   :module                                                 {:en "Study module"
                                                            :fi "Opintokokonaisuus"
                                                            :sv "Studiehelhet"}
   :base-education-other                                   {:en "Other eligibility for higher education"
                                                            :fi "Muu korkeakoulukelpoisuus"
                                                            :sv "Övrig högskolebehörighet"}
   :base-education-other-description                       {:en "Description of your other eligibility"
                                                            :fi "Kelpoisuuden kuvaus"
                                                            :sv "Beskrivning av behörigheten"}
   :secondary-completed-base-education                     {:en "Have you completed general upper secondary education or vocational qualification?"
                                                            :fi "Oletko suorittanut lukion/ylioppilastutkinnon tai ammatillisen tutkinnon? "
                                                            :sv "Har du avlagt gymnasiet/studentexamen eller yrkesinriktad examen?"}
   :secondary-completed-country                            {:en "Choose the country where you have completed your most recent qualification. If you have not yet completed a general upper secondary school syllabus/matriculation examination or vocational qualification but are in the process of doing so please choose the country where you will complete the qualification. NB: a vocational qualification can be a vocational upper secondary qualification school-level qualification post-secondary level qualification higher vocational level qualification further vocational qualification or specialist vocational qualification. Do not fill in the country where you have completed a higher education qualification."
                                                            :fi "Merkitse viimeisimmän tutkintosi suoritusmaa. Jos sinulla ei ole vielä lukion päättötodistusta/ylioppilastutkintoa tai ammatillista tutkintoa mutta olet suorittamassa sellaista valitse se maa jossa parhaillaan suoritat kyseistä tutkintoa. Huom: ammatillinen tutkinto voi olla ammatillinen perustutkinto kouluasteen opistoasteen tai ammatillisen korkea-asteen tutkinto ammatti-tai erikoisammattitutkinto. Älä merkitse tähän korkeakoulututkinnon suoritusmaata."
                                                            :sv "Ange land där din senaste examen avlagts. Om du ännu inte har avlagt gymnasiet/studentexamen eller yrkesinriktad examen men håller på att göra det välj då det land där du som bäst avlägger examen i fråga. Obs: yrkesinriktad examen kan vara yrkesinriktad grundexamen examen på skolnivå examen på institutsnivå yrkesinriktad examen på högre nivå yrkesexamen eller specialyrkesexamen. Ange inte här landet där du har avlagt högskoleexamen."}
   :choose-country                                         {:en "Choose country"
                                                            :fi "Valitse suoritusmaa"
                                                            :sv " Välj land där du avlagt examen"}
   :finnish-vocational-before-1995                         {:en "Have you completed a university or university of applied sciences ( prev.  polytechnic) degree in Finland before 1995?"
                                                            :fi "Oletko suorittanut suomalaisen ammattikorkeakoulu- tai yliopistotutkinnon ennen vuotta 1995?"
                                                            :sv "Har du avlagt en finländsk yrkeshögskole- eller universitetsexamen före år 1995?"}
   :finnish-vocational-before-1995-degree                  {:en "Write your university or university of applied sciences degree only if you have completed it before 1995. After that date the information of completed degrees will be received automatically from the higher education institutions. If you have completed a university/university of applied sciences degree or have received a study place in higher education in Finland after autumn 2014 your admission can be affected. More information on the quota for first -time applicants is available on https://studyinfo.fi/wp2/en/higher-education/applying/quota-for-first-time-applicants/"
                                                            :fi "Merkitse tähän suorittamasi korkeakoulututkinto vain siinä tapauksessa jos olet suorittanut sen ennen vuotta 1995. Sen jälkeen suoritetut tutkinnot saadaan automaattisesti korkeakouluilta. Suomessa suoritettu korkeakoulututkinto tai syksyllä 2014 tai sen jälkeen alkaneesta koulutuksesta vastaanotettu korkeakoulututkintoon johtava opiskelupaikka voivat vaikuttaa valintaan. Lue lisää [ensikertalaiskiintiöstä](https://opintopolku.fi/wp/valintojen-tuki/yhteishaku/korkeakoulujen-yhteishaku/ensikertalaiskiintio/)."
                                                            :sv "Ange här den högskoleexamen som du avlagt före år 1995. Examina som avlagts efter detta fås automatiskt av högskolorna. En högskoleexamen som avlagts i Finland eller en studieplats inom utbildning som leder till högskoleexamen som mottagits år 2014 eller senare kan inverka på antagningen. Läs mera om kvoten för förstagångssökande (https://studieinfo.fi/wp/stod-for-studievalet/gemensam-ansokan/gemensam-ansokan-till-hogskolor/kvot-for-forstagangssokande/)"}
   :name-of-degree                                         {:en "Name of the degree" :fi "Tutkinnon nimi" :sv "Examens namn"}
   :base-education-other-attachment                        {:en "Request for attachment on other eligibility for higher education "
                                                            :fi "Muun korkeakoulukelpoisuuden liitepyyntö"
                                                            :sv "Begäran om bilagor för övrig högskolebehörighet"}
   :required-for-statistics                                {:fi "Tämä tieto kysytään tilastointia varten."
                                                            :sv "Uppgiften insamlas för statistik."
                                                            :en "This is required for statistical reasons"}})

(def person-info-module-texts
  {:forenames            {:fi "Etunimet"
                          :sv "Förnamn"
                          :en "First/given names"}
   :main-forename        {:fi "Kutsumanimi"
                          :sv "Tilltalsnamn"
                          :en "Preferred first/given name"}
   :surname              {:fi "Sukunimi"
                          :sv "Efternamn"
                          :en "Last name"}
   :nationality          {:fi "Kansalaisuus"
                          :sv "Medborgarskap"
                          :en "Nationality"}
   :country-of-residence {:fi "Asuinmaa"
                          :sv "Bosättningsland"
                          :en "Country of residence"}
   :have-finnish-ssn     {:fi "Onko sinulla suomalainen henkilötunnus?"
                          :sv "Har du en finländsk personbeteckning?"
                          :en "Do you have a Finnish personal identity code?"}
   :ssn                  {:fi "Henkilötunnus"
                          :sv "Personbeteckning"
                          :en "Finnish personal identity code"}
   :gender               {:fi "Sukupuoli"
                          :sv "Kön"
                          :en "Gender"}
   :birth-date           {:fi "Syntymäaika"
                          :sv "Födelsetid"
                          :en "Date of birth"}
   :passport-number      {:fi "Passin numero"
                          :sv "Passnummer"
                          :en "Passport number"}
   :national-id-number   {:fi "Kansallinen ID-tunnus"
                          :sv "Nationellt ID-signum"
                          :en "National ID number"}
   :birthplace           {:fi "Syntymäpaikka ja -maa"
                          :sv "Födelseort och -land"
                          :en "Place and country of birth"}
   :email                {:fi "Sähköpostiosoite"
                          :sv "E-postadress"
                          :en "E-mail address"}
   :phone                {:fi "Matkapuhelin"
                          :sv "Mobiltelefonnummer"
                          :en "Mobile phone number"}
   :address              {:fi "Katuosoite"
                          :sv "Näradress"
                          :en "Address"}
   :home-town            {:fi "Kotikunta"
                          :sv "Hemkommun"
                          :en "Municipality of residence"}
   :city                 {:fi "Kaupunki ja maa"
                          :sv "Stad och land"
                          :en "City and country"}
   :postal-code          {:fi "Postinumero"
                          :sv "Postnummer"
                          :en "Postal code"}
   :postal-office        {:fi "Postitoimipaikka"
                          :sv "Postkontor"
                          :en "Town/city"}
   :language             {:fi "Äidinkieli"
                          :sv "Modersmål"
                          :en "Native language"}
   :label                {:fi "Henkilötiedot"
                          :sv "Personuppgifter"
                          :en "Personal information"}
   :label-amendment      {:fi "(Osio lisätään automaattisesti lomakkeelle)"
                          :sv "Denna del införs automatiskt i blanketten"
                          :en "The section will be automatically added to the application"}
   :date-formats         {:fi "pp.kk.vvvv"
                          :sv "dd.mm.åååå"
                          :en "dd.mm.yyyy"}})

(def email-default-texts
  {:email-submit-confirmation-template
   {:submit-email-subjects      {:fi "Opintopolku: hakemuksesi on vastaanotettu"
                                 :sv "Studieinfo: Din ansökan har mottagits"
                                 :en "Studyinfo: Your application has been received"}
    :with-application-period    {:fi "Voit katsella ja muokata hakemustasi hakuaikana yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan.\n\nÄlä vastaa tähän viestiin - viesti on lähetetty automaattisesti.\n\nYstävällisin terveisin <br/>\nOpintopolku\n"
                                 :sv "Om du vill ändra din ansökan, kan du göra ändringar via länken ovan under ansökningstiden. Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet om du använder en offentlig dator.\n\nOm du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och under ansökningstiden göra ändringarna i tjänsten Min Studieinfo. I tjänsten kan du också se ditt antagningsresultat och ta emot studieplatsen.\n\nSvara inte på detta meddelande, det har skickats automatiskt.\n\nMed vänliga hälsningar, <br/>\nStudieinfo\n"
                                 :en "If you wish to edit your application, you can use the link above and make the changes within the application period. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application.\n\nIf you have Finnish online banking credentials, an electronic ID-card or mobile certificate, you can also log in at [Studyinfo.fi](https://www.studyinfo.fi) and make the changes in the My Studyinfo -service within the application period. In addition to making changes to your application, if you have access to the My Studyinfo -service you can also view the admission results and confirm the study place.\n\nThis is an automatically generated email, please do not reply.\n\nBest regards, <br/>\nStudyinfo\n"}
    :without-application-period {:fi "Voit katsella ja muokata hakemustasi yllä olevan linkin kautta. Älä jaa linkkiä ulkopuolisille. Jos käytät yhteiskäyttöistä tietokonetta, muista kirjautua ulos sähköpostiohjelmasta.\n\nJos sinulla on verkkopankkitunnukset, mobiilivarmenne tai sähköinen henkilökortti, voit vaihtoehtoisesti kirjautua sisään [Opintopolku.fi](https://www.opintopolku.fi):ssä, ja tehdä muutoksia hakemukseesi Oma Opintopolku -palvelussa hakuaikana. Oma Opintopolku -palvelussa voit lisäksi nähdä valintojen tulokset ja ottaa opiskelupaikan vastaan.\n\nÄlä vastaa tähän viestiin - viesti on lähetetty automaattisesti.\n\nYstävällisin terveisin <br/>\nOpintopolku\n"
                                 :sv "Om du vill ändra din ansökan, kan du göra ändringar via länken ovan. Dela inte länken vidare till utomstående. Kom ihåg att logga ut från e-postprogrammet om du använder en offentlig dator.\n\nOm du har nätbankskoder, mobilcertifikat eller ett elektroniskt ID-kort, kan du alternativt logga in i [Studieinfo.fi](https://www.studieinfo.fi) och under ansökningstiden göra ändringarna i tjänsten Min Studieinfo. I tjänsten kan du också, se antagningsresultaten och ta emot studieplatsen.\n\nSvara inte på detta meddelande, det har skickats automatiskt.\n\nMed vänliga hälsningar, <br/>\nStudieinfo\n"
                                 :en "If you wish to edit your application, you can use the link above and make the changes within the application period. Do not share the link with others. If you are using a public or shared computer, remember to log out of the email application.\n\nIf you have Finnish online banking credentials, an electronic\nID-card or mobile certificate, you can also log in\nat [Studyinfo.fi](https://www.studyinfo.fi) and make the\nchanges in the My Studyinfo -service within the application period. In addition to making changes to your application, if you have access to the My Studyinfo -service you can also view the admission results and confirm the study place.\n\nThis is an automatically generated email, please do not reply.\n\nBest regards, <br/>\nStudyinfo\n"}}})

(def virkailija-texts
  {:valintakasittelymerkinta                        {:fi "Valintakäsittelymerkintä"
                                                     :sv "Notering om antagningsbehandling"
                                                     :en "Valintakäsittelymerkintä"}
   :valinnan-tila                                   {:fi "Valinnan tila"
                                                     :sv "Antagningens status"
                                                     :en "Valinnan tila"}
   :valinnan-tila-ladattu-valinnoista               {:fi "Valintatieto tuotu valintarekisteristä"
                                                     :sv "Antagningsuppgiften hämtad från antagningsregistret"
                                                     :en "Valintatieto tuotu valintarekisteristä"}
   :valinnan-tila-ladataan-valinnoista              {:fi "Valintatieto tuotu valintarekisteristä"
                                                     :sv "Antagningsuppgiften inhämtad från antagningsregistret"
                                                     :en "Valintatieto tuotu valintarekisteristä"}
   :arvosanat-peruskoulu                            {:fi "Arvosanat (peruskoulu)"
                                                     :sv "Vitsord (grundskola)"
                                                     :en "Arvosanat (peruskoulu)"}
   :arvosanat-lukio                                 {:fi "Arvosanat (lukio)"
                                                     :sv "Vitsord (gymnasium)"
                                                     :en "Arvosanat (lukio)"}
   :arvosana-aidinkieli-ja-kirjallisuus             {:fi "Äidinkieli ja kirjallisuus"
                                                     :sv "Modersmål och litteratur"
                                                     :en "Äidinkieli ja kirjallisuus"}
   :arvosana-a1-kieli                               {:fi "A1-kieli"
                                                     :sv "A1-språk"
                                                     :en "A1-kieli"}
   :arvosana-a2-kieli                               {:fi "A2-kieli"
                                                     :sv "A2-språk"
                                                     :en "A2-kieli"}
   :arvosana-b1-kieli                               {:fi "B1-kieli"
                                                     :sv "B1-språk"
                                                     :en "B1-kieli"}
   :arvosana-matematiikka                           {:fi "Matematiikka"
                                                     :sv "Matematik"
                                                     :en "Matematiikka"}
   :arvosana-biologia                               {:fi "Biologia"
                                                     :sv "Biologi"
                                                     :en "Biologia"}
   :arvosana-maantieto                              {:fi "Maantieto"
                                                     :sv "Geografi"
                                                     :en "Maantieto"}
   :arvosana-fysiikka                               {:fi "Fysiikka"
                                                     :sv "Fysik"
                                                     :en "Fysiikka"}
   :arvosana-kemia                                  {:fi "Kemia"
                                                     :sv "Kemi"
                                                     :en "Kemia"}
   :arvosana-terveystieto                           {:fi "Terveystieto"
                                                     :sv "Hälsokunskap"
                                                     :en "Terveystieto"}
   :arvosana-uskonto-tai-elamankatsomustieto        {:fi "Uskonto tai elämänkatsomustieto"
                                                     :sv "Religion eller livsåskådningskunskap"
                                                     :en "Uskonto tai elämänkatsomustieto"}
   :arvosana-historia                               {:fi "Historia"
                                                     :sv "Historia"
                                                     :en "Historia"}
   :arvosana-yhteiskuntaoppi                        {:fi "Yhteiskuntaoppi"
                                                     :sv "Samhällslära"
                                                     :en "Yhteiskuntaoppi"}
   :arvosana-musiikki                               {:fi "Musiikki"
                                                     :sv "Musik"
                                                     :en "Musiikki"}
   :arvosana-kuvataide                              {:fi "Kuvataide"
                                                     :sv "Bildkonst"
                                                     :en "Kuvataide"}
   :arvosana-kasityo                                {:fi "Käsityö"
                                                     :sv "Slöjd"
                                                     :en "Käsityö"}
   :arvosana-liikunta                               {:fi "Liikunta"
                                                     :sv "Gymnastik"
                                                     :en "Liikunta"}
   :arvosana-kotitalous                             {:fi "Kotitalous"
                                                     :sv "Huslig ekonomi"
                                                     :en "Kotitalous"}
   :arvosanat-info                                  {:fi "Merkitse arvosanat sitä todistuksesta, jolla haet koulutukseen. Korotetut arvosanat voit merkitä, mikäli olet saanut korotuksista virallisen todistuksen. Jos olet suorittanut lukion oppimäärän, et voi hakea perusopetuksen päättötodistuksella. Ammatillisella perustutkinnolla et voi hakea. Oppilaitokset tarkistavat todistukset hyväksytyksi tulleilta hakijoilta.\n\nHuom! Jos haet perusopetuksen päättötodistuksella, muista täyttää myös valinnaisaineiden arvosanat. Valinnaisaineiden arvosanat merkitään vain mikäli olet opiskellut niitä vähintään kaksi vuosiviikkotuntia perusopetuksen vuosiluokkien 7-9 aikana."
                                                     :sv "Ange vitsorden från det betyg, enligt vilket du söker till utbildningen. Du kan ange höjda vitsord, om du har fått ett officiellt intyg över höjningen. Om du har avlagt gymnasiets lärokurs, kan du inte söka med avgångsbetyget över grundläggande utbildning. Du kan inte söka enligt en yrkesinriktad grundexamen. Läroanstalterna kontrollerar betygen för de sökande som godkänns till utbildning.\n\nOBS! Om du söker med den grundläggande utbildningens avgångsbetyg ska du komma ihåg att fylla i vitsorden över valfria ämnen. Ange ändå vitsorden bara om du studerat ämnet i minst två årsveckotimmar under årsklasser 7-9."
                                                     :en "Merkitse arvosanat sitä todistuksesta, jolla haet koulutukseen. Korotetut arvosanat voit merkitä, mikäli olet saanut korotuksista virallisen todistuksen. Jos olet suorittanut lukion oppimäärän, et voi hakea perusopetuksen päättötodistuksella. Ammatillisella perustutkinnolla et voi hakea. Oppilaitokset tarkistavat todistukset hyväksytyksi tulleilta hakijoilta.\n\nHuom! Jos haet perusopetuksen päättötodistuksella, muista täyttää myös valinnaisaineiden arvosanat. Valinnaisaineiden arvosanat merkitään vain mikäli olet opiskellut niitä vähintään kaksi vuosiviikkotuntia perusopetuksen vuosiluokkien 7-9 aikana."}
   :show-hakukierros-paattynyt                      {:fi "Näytä haut joissa hakukierros päättynyt"
                                                     :sv "Visa ansökningar där ansökningsperioden har avslutats"
                                                     :en "Näytä haut joissa hakukierros päättynyt"}
   :hide-hakukierros-paattynyt                      {:fi "Piilota haut joissa hakukierros päättynyt"
                                                     :sv "Dölj ansökningar där ansökningsperioden har avslutats"
                                                     :en "Piilota haut joissa hakukierros päättynyt"}
   :active                                          {:fi "Aktiivinen"
                                                     :sv "Aktiv"
                                                     :en "Active"}
   :add                                             {:fi "Lisää"
                                                     :sv "Lägg till"
                                                     :en "Add more"}
   :adjacent-fieldset                               {:fi "Vierekkäiset tekstikentät"
                                                     :sv "Parallella textfält"
                                                     :en "Parallel text areas"}
   :all                                             {:fi "Kaikki"
                                                     :sv "Alla"
                                                     :en "All"}
   :all-hakukohteet                                 {:fi "Kaikki hakukohteet"
                                                     :sv "Alla ansökningsmål"
                                                     :en "All study programmes"}
   :allow-invalid-koodis                            {:fi "Sisällytä päättyneet koodit"
                                                     :sv "Inbegrip utgångna koder"
                                                     :en "EN: Sisällytä päättyneet koodit"}
   :alphabetically                                  {:fi "Aakkosjärjestyksessä"
                                                     :sv "I alfabetisk ordning"
                                                     :en "In alphabetical order"}
   :answers                                         {:fi "vastausta:"
                                                     :sv "svar:"
                                                     :en "answers:"}
   :applicant                                       {:fi "Hakija"
                                                     :sv "Sökande"
                                                     :en "Applicant"}
   :applicant-will-receive-following-email          {:fi "Hakija saa allaolevan viestin sähköpostilla hakemuksen lähettämisen jälkeen lähettäjältä"
                                                     :sv "Sökande får nedanstående meddelande i sin e-post efter att hen har skickat sin ansökan"
                                                     :en "EN: Hakija saa allaolevan viestin sähköpostilla hakemuksen lähettämisen jälkeen lähettäjältä"}
   :application                                     {:fi "hakemus"
                                                     :sv "ansökan"
                                                     :en "Application"}
   :application-oid-here                            {:fi "Tähän tulee hakemusnumero, hakutoiveet, puuttuvat liitepyynnöt ja muokkauslinkki"
                                                     :sv "Här visas ansökningsnummer, ansökningsönskemål, begäran om bilagor som saknas och bearbetningslänken"
                                                     :en "EN: Tähän tulee hakemusnumero, hakutoiveet, puuttuvat liitepyynnöt ja muokkauslinkki"}
   :application-options                             {:fi "hakukohdetta"
                                                     :sv "ansökningsmål"
                                                     :en "study programme"}
   :application-received                            {:fi "Hakemus vastaanotettu"
                                                     :sv "Ansökan har mottagits"
                                                     :en "Application submitted"}
   :application-state                               {:fi "Hakemuksen tila"
                                                     :sv "Ansökans status"
                                                     :en "Status of application"}
   :applications                                    {:fi "hakemusta"
                                                     :sv "ansökningar"
                                                     :en "application"}
   :view-applications                               {:fi "Näytä oppijan hakemukset"
                                                     :sv "Visa sökandes ansökningar"
                                                     :en "Show applications"}
   :valintojen-toteuttaminen                        {:fi "Valintojen toteuttaminen"
                                                     :sv "Förverkligandet av antagningar"
                                                     :en "Valintojen toteuttaminen"}
   :applications-panel                              {:fi "Hakemukset"
                                                     :sv "Ansökningar"
                                                     :en "Applications"}
   :asiointikieli                                   {:fi "Asiointikieli"
                                                     :sv "Kontaktspråk"
                                                     :en "Contact language"}
   :attachment                                      {:fi "Liitepyyntö"
                                                     :sv "Begäran om bilagor"
                                                     :en "Attachment"}
   :attachment-info-text                            {:fi "Liitepyyntö sisältää ohjetekstin"
                                                     :sv "Begäran om bilagor innehåller anvisningar"
                                                     :en "EN: Liitepyyntö sisältää ohjetekstin"}
   :mail-attachment-text                            {:fi "Postitettava liitepyyntö"
                                                     :sv "Begäran om bilagor som kan postas"
                                                     :en "EN: Postitettava liitepyyntö"}
   :attachment-name                                 {:fi "Liitteen nimi"
                                                     :sv "Bilagans namn"
                                                     :en "Title of an attachment"}
   :attachment-deadline                             {:fi "Valinnainen toimituspäivämäärä (pp.kk.vvvv hh:mm)"
                                                     :sv "Valfri leveransdag (pp.kk.vvvv hh:mm)"
                                                     :en "EN: Valinnainen toimituspäivämäärä (pp.kk.vvvv hh:mm)"}
   :attachments                                     {:fi "Liitepyynnöt"
                                                     :sv "Begäran om bilagor"
                                                     :en "Attachments"}
   :submitted-content-search-placeholder            {:fi "Hae kysymyksellä tai liitepyynnöllä..."
                                                     :sv "Sök enligt fråga eller begäran om bilaga..."
                                                     :en "EN: Hae kysymyksellä tai liitepyynnöllä..."}
   :submitted-content-search-label                  {:fi "Kysymys / liitepyyntö"
                                                     :sv "Fråga / begäran om bilaga"
                                                     :en "EN: Kysymys / liitepyyntö"}
   :base-education                                  {:fi "Pohjakoulutus"
                                                     :sv "Grundutbildning"
                                                     :en "Education background"}
   :base-education-module                           {:fi "Pohjakoulutusmoduuli"
                                                     :sv "Grundutbildningsmodul"
                                                     :en "EN: Pohjakoulutusmoduuli"}
   :cannot-display-file-type-in-attachment-skimming {:fi "Tätä liitettä ei valitettavasti voida näyttää esikatselussa, mutta voit ladata sen tästä tiedostona."
                                                     :sv "Denna bilaga kan tyvärr inte visas i förhandsgranskningen, men du kan ladda ner bilagan som en fil."
                                                     :en "This attachment can't unfortunately be shown but you can download it here."}
   :partial-preview-in-attachment-skimming          {:fi "Voit ladata koko liitteen tästä. Esikatselussa näytetään sivut "
                                                     :sv "Du kan ladda ner hela bilaga här. I förhandsvisningen visas sidorna "
                                                     :en "Voit ladata koko liitteen tästä. Esikatselussa näytetään sivut (en) "}
   :change                                          {:fi "Muuta"
                                                     :sv "Byt"
                                                     :en "Change"}
   :change-organization                             {:fi "Vaihda organisaatio"
                                                     :sv "Byt organisation"
                                                     :en "Change the organization"}
   :changed                                         {:fi "muutti"
                                                     :sv "ändrades av"
                                                     :en "changed"}
   :changes                                         {:fi "muutosta"
                                                     :sv "ändringar"
                                                     :en "changes"}
   :checking                                        {:fi "Tarkastetaan"
                                                     :sv "Kontrolleras"
                                                     :en "Inspecting"}
   :choose-user-rights                              {:fi "Valitse käyttäjän oikeudet"
                                                     :sv "Välj användarrättigheter"
                                                     :en "Choose the users access"}
   :close                                           {:fi "sulje"
                                                     :sv "stäng"
                                                     :en "close"}
   :collapse-info-text                              {:fi "Pienennä pitkä ohjeteksti"
                                                     :sv "Visa mindre av långa anvisningar"
                                                     :en "EN: Pienennä pitkä ohjeteksti"}
   :compare                                         {:fi "Vertaile"
                                                     :sv "Jämför"
                                                     :en "Compare"}
   :confirm-change                                  {:fi "Vahvista muutos"
                                                     :sv "Bekräfta ändringen"
                                                     :en "Confirm the change"}
   :confirm-delete                                  {:fi "Vahvista poisto"
                                                     :sv "Bekräfta raderingen"
                                                     :en "Confirm the deletion"}
   :cancel-remove                                   {:fi "Älä poista"
                                                     :sv "Radera inte"
                                                     :en "Cancel remove"}
   :confirm-cut                                     {:fi "Vahvista leikkaus"
                                                     :sv "Bekräfta utklippning"
                                                     :en "Confirm the cut"}
   :cancel-cut                                      {:fi "Älä leikkaa"
                                                     :sv "Klipp inte ut"
                                                     :en "Cancel cut"}
   :cancel-copy                                     {:fi "Älä kopio"
                                                     :sv "Kopiera inte"
                                                     :en "Cancel copy"}
   :only-selected-hakukohteet                       {:fi "vain valituille hakukohteille"
                                                     :sv "till valda ansökningsmål"
                                                     :en "only selected study programmes"}
   :confirmation-sent                               {:fi "Vahvistussähköposti lähetetty hakijalle"
                                                     :sv "E-post med bekräftelse har skickats till sökande"
                                                     :en "Confirmation email has been sent"}
   :contains-fields                                 {:fi "Sisältää kentät:"
                                                     :sv "Innehåller fälten:"
                                                     :en "Includes the areas:"}
   :copy-form                                       {:fi "Kopioi lomake"
                                                     :sv "Kopiera blanketten"
                                                     :en "Copy the form"}
   :form-contains-applications?                     {:fi "Lomakkeella on hakemuksia"
                                                     :sv "Innehåller ansökningar"
                                                     :en "Form contains applications"}
   :cut-element                                     {:fi "Leikkaa"
                                                     :sv "Klipp ut"
                                                     :en "Cut"}
   :paste-element                                   {:fi "Liitä"
                                                     :sv "Klistra in"
                                                     :en "Paste"}
   :copy-element                                    {:fi "Kopioi"
                                                     :sv "Kopiera"
                                                     :en "Copy"}
   :copy-answer-id                                  {:fi "Kopioi vastauksen tunniste leikepöydälle"
                                                     :sv "Kopiera svarstaggen till klippbordet"
                                                     :en "EN: Kopioi vastauksen tunniste leikepöydälle"}
   :copy-question-id                                {:fi "Kopioi kysymyksen tunniste leikepöydälle"
                                                     :sv "Kopiera svarstaggen till klippbordet"
                                                     :en "EN: Kopioi kysymyksen tunniste leikepöydälle"}
   :created-by                                      {:fi "Luonut"
                                                     :sv "Grundad av"
                                                     :en "Created by"}
   :custom-choice-label                             {:fi "Omat vastausvaihtoehdot"
                                                     :sv "Egna svarsalternativ"
                                                     :en "Own answer options"}
   :decimals                                        {:fi "desimaalia"
                                                     :sv "decimaler"
                                                     :en "decimals"}
   :delete-form                                     {:fi "Poista lomake"
                                                     :sv "Ta bort blanketten"
                                                     :en "Delete the form"}
   :cancel-form-delete                              {:fi "Älä poista"
                                                     :sv "Radera inte"
                                                     :en "Don't remove"}
   :did                                             {:fi "teki"
                                                     :sv "har gjort"
                                                     :en "has made"}
   :diff-from-changes                               {:fi "Vertailu muutoksesta"
                                                     :sv "Jämför ändringen"
                                                     :en "Compare the change"}
   :diff-added                                      {:fi "Lisätty"
                                                     :sv "Lagts till"
                                                     :en "Added"}
   :diff-removed                                    {:fi "Poistettu"
                                                     :sv "Raderats"
                                                     :en "Removed"}
   :dropdown                                        {:fi "Pudotusvalikko"
                                                     :sv "Rullgardinsmeny"
                                                     :en "Dropdown"}
   :dropdown-koodisto                               {:fi "Pudotusvalikko, koodisto"
                                                     :sv "Rullgardinsmeny, kodregister"
                                                     :en "Dropdown, codes"}
   :edit-application                                {:fi "Muokkaa hakemusta"
                                                     :sv "Bearbeta ansökan"
                                                     :en "Edit the application"}
   :edit-application-with-rewrite                   {:fi "Muokkaa hakemusta rekisterinpitäjänä"
                                                     :sv "Bearbeta ansökan som registerförare"
                                                     :en "Muokkaa hakemusta rekisterinpitäjänä"}
   :edit-applications-rights-panel                  {:fi "Hakemusten arviointi"
                                                     :sv "Utvärdering av ansökningar"
                                                     :en "Evaluation of applications"}
   :edit-valinta-rights-panel                       {:fi "Valinnan tuloksen muokkaus"
                                                     :sv "Bearbeta antagningsresultat"
                                                     :en "EN: Valinnan tuloksen muokkaus"}
   :edit-email-templates                            {:fi "Muokkaa sähköpostipohjia"
                                                     :sv "Bearbeta e-postmallar"
                                                     :en "Edit the email templates"}
   :edit-link-sent-automatically                    {:fi "Muokkauslinkki lähtee viestin mukana automaattisesti"
                                                     :sv "Bearbetningslänken skickas automatiskt med meddelandet"
                                                     :en "The edit link will be sent automatically"}
   :editable-content-beginning                      {:fi "Muokattava osuus (viestin alku)"
                                                     :sv "Del som ska bearbetas (början av meddelandet)"
                                                     :en "EN: Muokattava osuus (viestin alku)"}
   :editable-content-ending                         {:fi "Muokattava osuus (viestin loppu)"
                                                     :sv "Del som ska bearbetas (slutet av meddelandet)"
                                                     :en "EN: Muokattava osuus (viestin loppu)"}
   :editable-content-title                          {:fi "Muokattava osuus (otsikko)"
                                                     :sv "Del som ska bearbetas (rubrik)"
                                                     :en "EN: Muokattava osuus (otsikko)"}
   :ehdollisuus                                     {:fi "Ehdollisuus"
                                                     :sv "Villkorlighet"
                                                     :en "EN: Ehdollisuus"}
   :ehdollisesti-hyvaksyttavissa                    {:fi "Ehdollinen"
                                                     :sv "Villkorlig"
                                                     :en "EN: Ehdollinen"}
   :ei-ehdollisesti-hyvaksyttavissa                 {:fi "Ei ehdollinen"
                                                     :sv "Icke-villkorlig"
                                                     :en "EN: Ei ehdollinen"}
   :eligibility                                     {:fi "Hakukelpoisuus:"
                                                     :sv "Ansökningsbehörighet:"
                                                     :en "Criteria for eligibility"}
   :eligibility-explanation                         {:fi "Kelpoisuusmerkinnän selite"
                                                     :sv "Förklaring till behörighetsanteckningen"
                                                     :en "Explanation of eligibility"}
   :eligibility-set-automatically                   {:fi "Hakukelpoisuus asetettu automaattisesti"
                                                     :sv "Ansökningsbehörigheten har satts automatiskt"
                                                     :en "Eligibility set automatically"}
   :payment-obligation                              {:fi "Maksuvelvollisuus"
                                                     :sv "Betalningsskyldighet"
                                                     :en "Obligated to pay"}
   :payment-obligation-set-automatically            {:fi "Maksuvelvollisuus asetettu automaattisesti"
                                                     :sv "Betalningsskyldighet har ställts automatiskt"
                                                     :en "Payment obligation set automatically"}
   :email-content                                   {:fi "Sähköpostiviestin sisältö"
                                                     :sv "E-postmeddelandets innehåll"
                                                     :en "Content of the email"}
   :empty-option                                    {:fi "Ei vastausta"
                                                     :sv "Inget svar"
                                                     :en "No answer"}
   :english                                         {:fi "Englanti"
                                                     :sv "Engelska"
                                                     :en "English"}
   :ensisijaisesti                                  {:fi "Hakenut ensisijaisesti"
                                                     :sv "Sökt i förstahand"
                                                     :en "First priority applicants"}
   :ensisijaisuus                                   {:fi "Ensisijaisuus"
                                                     :sv "I förstahand"
                                                     :en "First priority"}
   :error                                           {:fi "Virhe"
                                                     :sv "Fel"
                                                     :en "Error"}
   :events                                          {:fi "Tapahtumat"
                                                     :sv "Händelser"
                                                     :en "Events"}
   :filter-applications                             {:fi "Rajaa hakemuksia"
                                                     :sv "Avgränsa ansökningar"
                                                     :en "Filter the applications"}
   :filter-by-state                                 {:fi "Rajaa tilan mukaan"
                                                     :sv "Avgränsa enligt status"
                                                     :en "EN: Rajaa tilan mukaan"}
   :filters-apply-button                            {:fi "Ota käyttöön"
                                                     :sv "Använd"
                                                     :en "Apply"}
   :filters-cancel-button                           {:fi "Peruuta"
                                                     :sv "Annullera"
                                                     :en "Cancel"}
   :finnish                                         {:fi "Suomi"
                                                     :sv "Finska"
                                                     :en "Finnish"}
   :followups                                       {:fi "Lisäkysymykset"
                                                     :sv "Tilläggsfrågor"
                                                     :en "Extra questions"}
   :for-hakukohde                                   {:fi "hakukohteelle"
                                                     :sv "för ansökningsmålet"
                                                     :en "for study programme"}
   :form                                            {:fi "Lomake"
                                                     :sv "Blankett"
                                                     :en "Form"}
   :form-edit-rights-panel                          {:fi "Lomakkeiden muokkaus"
                                                     :sv "Bearbetning av blanketter"
                                                     :en "Form editing"}
   :form-locked                                     {:fi "Lomakkeen muokkaus on estetty"
                                                     :sv "Du kan inte bearbeta blanketten"
                                                     :en "You can't edit the form anymore"}
   :form-name                                       {:fi "Lomakkeen nimi"
                                                     :sv "Blankettens namn"
                                                     :en "Name of the application form"}
   :form-section                                    {:fi "Lomakeosio"
                                                     :sv "Blankettdel"
                                                     :en "Section"}
   :form-outdated                                   {:fi "Lomakkeesta on uudempi versio!"
                                                     :sv "Det finns en ny version av sökandens blankett!"
                                                     :en "There is a new version of the application"}
   :show-newest-version                             {:fi "Näytä lomake uusimmalla versiolla"
                                                     :sv "Visa blanketten i nyaste version"
                                                     :en "Show the latest version of the form"}
   :forms                                           {:fi "Lomakkeet"
                                                     :sv "Blanketter"
                                                     :en "Applications"}
   :forms-panel                                     {:fi "Lomakkeet"
                                                     :sv "Blanketter"
                                                     :en "Applications"}
   :from-applicant                                  {:fi "Hakijalta"
                                                     :sv "Av sökande"
                                                     :en "From applicant"}
   :from-state                                      {:fi "Tilasta"
                                                     :sv "Ur status"
                                                     :en "From status to"}
   :group                                           {:fi "ryhmä"
                                                     :sv "grupp"
                                                     :en "Group"}
   :group-header                                    {:fi "Kysymysryhmän otsikko"
                                                     :sv "Rubrik för frågegrupp"
                                                     :en "Header of the question group"}
   :hakukohde-info                                  {:fi "Tässä hakija voi valita hakukohteet. Hakukohteiden määrä ja priorisointi määritetään haun asetuksissa."
                                                     :sv "Sökande kan här välja ansökningsmål. Antalet ansökningsmål och prioriteringen definieras i inställningarna för ansökan."
                                                     :en "EN: Tässä hakija voi valita hakukohteet. Hakukohteiden määrä ja priorisointi määritetään haun asetuksissa."}
   :hakukohteet                                     {:fi "Hakukohteet"
                                                     :sv "Ansökningsmål"
                                                     :en "Study programmes"}
   :hakukohderyhmat                                 {:fi "Hakukohderyhmät"
                                                     :sv "Ansökningsmålsgrupp"
                                                     :en "Study programme groups"}
   :search-hakukohde-placeholder                    {:fi "Etsi hakukohteita ja hakukohderyhmiä"
                                                     :sv "Sök ansökningsmål och ansökningsmålsgrupper"
                                                     :en "Search for study programmes and study programme groups"}
   :handling-notes                                  {:fi "Käsittelymerkinnät"
                                                     :sv "Anteckningar om behandling"
                                                     :en "Notes"}
   :hide-options                                    {:fi "Sulje vastausvaihtoehdot"
                                                     :sv "Stäng svarsalternativen"
                                                     :en "Hide the options"}
   :identified                                      {:fi "Yksilöidyt"
                                                     :sv "Identifierade"
                                                     :en "Identified"}
   :identifying                                     {:fi "Yksilöinti"
                                                     :sv "Identifiering"
                                                     :en "Identifying"}
   :incomplete                                      {:fi "Kesken"
                                                     :sv "Inte färdig"
                                                     :en "Incomplete"}
   :ineligibility-reason                            {:fi "Hylkäyksen syy"
                                                     :sv "Orsak till avslag"
                                                     :en "Reason for ineligibility"}
   :info-addon                                      {:fi "Kysymys sisältää ohjetekstin"
                                                     :sv "Frågan innehåller anvisningar"
                                                     :en "EN: Kysymys sisältää ohjetekstin"}
   :info-element                                    {:fi "Infoteksti"
                                                     :sv "Infotext"
                                                     :en "Info element"}
   :information-request-sent                        {:fi "Täydennyspyyntö lähetetty"
                                                     :sv "Begäran om komplettering har skickats"
                                                     :en "Information request email has been sent"}
   :mass-information-request-sent                   {:fi "Viesti lähetetty"
                                                     :sv "Meddelandet har skickats"
                                                     :en "Message has been sent"}
   :integer                                         {:fi "kokonaisluku"
                                                     :sv "heltal"
                                                     :en "integer"}
   :kk-base-education-module                        {:fi "Pohjakoulutusmoduuli (kk-yhteishaku)"
                                                     :sv "Grundutbildningsmodul (Gea till högskolor)"
                                                     :en "EN: Pohjakoulutusmoduuli (kk-yhteishaku)"}
   :koodisto                                        {:fi "Koodisto"
                                                     :sv "Kodregister"
                                                     :en "Codes"}
   :koulutusmarkkinointilupa                        {:fi "Koulutusmarkkinointilupa"
                                                     :sv "Tillstånd för utbildningsmarknadsföring"
                                                     :en "EN: Koulutusmarkkinointilupa"}
   :last-modified                                   {:fi "Viimeksi muokattu"
                                                     :sv "Senast bearbetad"
                                                     :en "Modified last"}
   :last-modified-by                                {:fi "viimeksi muokannut"
                                                     :sv "Senast bearbetad av"
                                                     :en "Last modified by"}
   :link-to-form                                    {:fi "Linkki lomakkeeseen"
                                                     :sv "Länk till blanketten"
                                                     :en "Link to the form"}
   :link-to-feedback                                {:fi "Linkki palautteeseen"
                                                     :sv "Länk till responsen"
                                                     :en "Link to the feedback"}
   :load-excel                                      {:fi "Lataa Excel"
                                                     :sv "Ladda ner Excel"
                                                     :en "Load excel"}
   :load-attachments                                {:fi "Lataa liitteet"
                                                     :sv "Ladda ner bilagor"
                                                     :en "Load attachments"}
   :load-attachment-in-skimming                     {:fi "lataa"
                                                     :sv "ladda"
                                                     :en "download"}
   :select-all                                      {:fi "Valitse kaikki"
                                                     :sv "Välj alla"
                                                     :en "Select all"}
   :lock-form                                       {:fi "Lukitse lomake"
                                                     :sv "Lås blanketten"
                                                     :en "Lock the form"}
   :logout                                          {:fi "Kirjaudu ulos"
                                                     :sv "Logga ut"
                                                     :en "Log out"}
   :lupa-sahkoiseen-asiointiin                      {:fi "Sähköisen asioinnin lupa"
                                                     :sv "Tillstånd för elektronisk kontakt"
                                                     :en "EN: Sähköisen asioinnin lupa"}
   :lupatiedot                                      {:fi "Lupatiedot"
                                                     :sv "Tillståndsuppgifter"
                                                     :en "Permissions"}
   :mass-edit                                       {:fi "Massamuutos"
                                                     :sv "Massändring"
                                                     :en "Mass editing"}
   :excel-request                                   {:fi "Excel"
                                                     :sv "Excel"
                                                     :en "Excel"}
   :excel-included-ids                              {:fi "Exceliin sisältyvät tunnisteet:"
                                                     :sv "Identifikationer som ingår i excelfilen:"
                                                     :en "Exceliin sisältyvät tunnisteet:"}
   :excel-include-all-placeholder                   {:fi "Kaikki tunnisteet"
                                                     :sv "Alla identifikationer"
                                                     :en "Kaikki tunnisteet"}
   :mass-information-request                        {:fi "Massaviesti"
                                                     :sv "Massmeddelande"
                                                     :en "Mass message"}
   :mass-information-request-confirm-n-messages     {:fi "Vahvista %d viestin lähetys"
                                                     :sv "Bekräfta att %d meddelanden kommer att skickas"
                                                     :en "Confirm sending %d messages"}
   :mass-information-request-email-n-recipients     {:fi "Lähetä sähköposti %d hakijalle:"
                                                     :sv "Skicka e-post till %d sökande:"
                                                     :en "Send email to %d applicants:"}
   :mass-information-request-messages-sent          {:fi "Viestit lisätty lähetysjonoon!"
                                                     :sv "Meddelandena har lagts till i utskickskön!"
                                                     :en "Messages have been sent!"}
   :mass-information-request-send                   {:fi "Lähetä"
                                                     :sv "Skicka:"
                                                     :en "Send:"}
   :mass-information-request-sending-messages       {:fi "Käsitellään viestejä..."
                                                     :sv "Meddelanden behandlas..."
                                                     :en "Sending the messages"}
   :mass-information-request-subject                {:fi "Aihe:"
                                                     :sv "Ämne:"
                                                     :en "Subject:"}
   :max-characters                                  {:fi "Max. merkkimäärä"
                                                     :sv "Max. teckenantal"
                                                     :en "Max. characters"}
   :md-help-bold                                    {:fi "**lihavoitava sisältö**"
                                                     :sv "**innehåll med fetstil**"
                                                     :en "**bold content**"}
   :md-help-cursive                                 {:fi "*kursivoitava sisältö*"
                                                     :sv "*med kursiv stil*"
                                                     :en "*cursive content*"}
   :md-help-link                                    {:fi "[linkin teksti](http://linkin osoite)"
                                                     :sv "[länkens text](http://länkens adress)"
                                                     :en "[link text](http://link address)"}
   :md-help-more                                    {:fi "Lisää muotoiluohjeita"
                                                     :sv "Lägg till anvisningar för utformning"
                                                     :en "More instructions"}
   :md-help-title                                   {:fi "# otsikko (# ylin - ###### alin)"
                                                     :sv "# rubrik (# högsta - ###### lägst)"
                                                     :en "# title (# highest - ###### lowest)"}
   :message-preview                                 {:fi "Viestin esikatselu"
                                                     :sv "Förhandsgranska meddelandet"
                                                     :en "Preview the message"}
   :more-results-refine-search                      {:fi "Lataa lisää tuloksia"
                                                     :sv "Ladda mera"
                                                     :en "Load more results"}
   :multiple-answers                                {:fi "Vastaaja voi lisätä useita vastauksia"
                                                     :sv "Du kan ge flera svar"
                                                     :en "EN: Vastaaja voi lisätä useita vastauksia"}
   :multiple-choice                                 {:fi "Lista, monta valittavissa"
                                                     :sv "Flervalslista"
                                                     :en "Multiple choice"}
   :multiple-choice-koodisto                        {:fi "Lista, monta valittavissa, koodisto"
                                                     :sv "Flervalslista, kodregister"
                                                     :en "Multiple choice, codes"}
   :multiple-organizations                          {:fi "Useita organisaatioita"
                                                     :sv "Flera organisationer"
                                                     :en "Multiple organizations"}
   :new-form                                        {:fi "Uusi lomake"
                                                     :sv "Ny blankett"
                                                     :en "New form"}
   :no-search-hits                                  {:fi "Ei hakutuloksia"
                                                     :sv "Inga sökresultat"
                                                     :en "EN: Ei hakutuloksia"}
   :no-organization                                 {:fi "Ei organisaatiota"
                                                     :sv "Ingen organisation"
                                                     :en "No organization"}
   :notes                                           {:fi "Muistiinpanot"
                                                     :sv "Anteckningar"
                                                     :en "Notes"}
   :of-form                                         {:fi "Lomakkeen"
                                                     :sv "Blankettens"
                                                     :en "Form's"}
   :of-hakukohde                                    {:fi "Hakukohteen"
                                                     :sv "Ansökningsmålets"
                                                     :en "Study programme's"}
   :only-numeric                                    {:fi "Kenttään voi täyttää vain numeroita"
                                                     :sv "Endast siffror i fältet"
                                                     :en "Only numbers"}
   :numeric-range                                   {:fi "Arvoalueen rajaus"
                                                     :sv "Avgränsning av värdeområde"
                                                     :en "Arvoalueen rajaus"}
   :open                                            {:fi "avaa"
                                                     :sv "öppna"
                                                     :en "Open"}
   :options                                         {:fi "Vastausvaihtoehdot"
                                                     :sv "Svarsalternativ"
                                                     :en "Options"}
   :passive                                         {:fi "Passiivinen"
                                                     :sv "Passiv"
                                                     :en "Inactive"}
   :person-completed-education                      {:fi "Henkilön suoritukset"
                                                     :sv "Personens prestationer"
                                                     :en "Applicant's exams in Finland"}
   :person-info-module-onr                          {:fi "Opiskelijavalinta"
                                                     :en "Opiskelijavalinta"
                                                     :sv "Studerandeantagning"}
   :person-info-module-muu                          {:fi "Muu käyttö"
                                                     :en "Muu käyttö"
                                                     :sv "Annat bruk"}
   :metadata-not-found                              {:fi "Hakijan liitteitä ei löytynyt"
                                                     :sv "Sökandes bilagor hittades inte"
                                                     :en "Applicant's attachements can't be found"}
   :person-not-individualized                       {:fi "Hakijaa ei ole yksilöity."
                                                     :sv "Sökande har inte identifierats."
                                                     :en "Applicant isn't identified."}
   :individualize-in-henkilopalvelu                 {:fi "Tee yksilöinti henkilöpalvelussa."
                                                     :sv "Identifiera i persontjänsten."
                                                     :en "Identify the applicant."}
   :operation-failed                                {:fi "Toiminto epäonnistui"
                                                     :sv "Funktionen misslyckades"
                                                     :en "EN: Toiminto epäonnistui"}
   :creating-henkilo-failed                         {:fi "Henkilön luonti ei ole valmistunut! Tarkista hakemuksen nimitiedot (esim. kutsumanimi on yksi etunimistä)."
                                                     :sv "Att bilda personen är inte färdig. Kontrollera att namnuppgifterna är korrekta (t.ex. att tilltalsnamnet ingår)"
                                                     :en "EN: Henkilön luonti ei ole valmistunut! Tarkista hakemuksen nimitiedot (esim. kutsumanimi on yksi etunimistä)"}
   :henkilo-info-incomplete                         {:fi "Hakemuksen lataus epäonnistui puuttuvien henkilötietojen vuoksi."
                                                     :sv "Att ladda ner ansökan misslyckades p g a bristfälliga personuppgifter."
                                                     :en "EN: Hakemuksen lataus epäonnistui puuttuvien henkilötietojen vuoksi."}
   :review-in-henkilopalvelu                        {:fi "Tarkasta henkilön tiedot henkilöpalvelussa."
                                                     :sv "Kontrollera personens uppgifter i persontjänsten."
                                                     :en "EN: Tarkasta henkilön tiedot henkilöpalvelussa."}
   :pohjakoulutus_am                                {:fi "Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                                                     :sv "I Finland avlagd yrkesinriktad grundexamen, examen på skolnivå, institutnivå eller inom yrkesutbildning på högre nivå"
                                                     :en "EN: Suomessa suoritettu ammatillinen perustutkinto, kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"}
   :pohjakoulutus_amp                               {:fi "Suomessa suoritettu ammatillinen perustutkinto"
                                                     :sv "Yrkesinriktad grundexamen som avlagts i Finland"
                                                     :en "Vocational upper secondary qualification completed in Finland (ammatillinen perustutkinto)"}
   :pohjakoulutus_amt                               {:fi "Suomessa suoritettu ammatti- tai erikoisammattitutkinto"
                                                     :sv "I Finland avlagd yrkes- eller specialyrkesexamen"
                                                     :en "EN: Suomessa suoritettu ammatti- tai erikoisammattitutkinto"}
   :pohjakoulutus_amv                               {:fi "Suomessa suoritettu kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto"
                                                     :sv "Yrkesinriktad examen på skolnivå, examen på institutsnivå eller yrkesinriktad examen på högre nivå som avlagts i Finland"
                                                     :en "Former vocational qualification completed in Finland (kouluasteen, opistoasteen tai ammatillisen korkea-asteen tutkinto)"}
   :pohjakoulutus_avoin                             {:fi "Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"
                                                     :sv "Studier inom öppen högskoleundervisning som högskolan förutsätter"
                                                     :en "EN: Korkeakoulun edellyttämät avoimen korkeakoulun opinnot"}
   :pohjakoulutus_kk                                {:fi "Suomessa suoritettu korkeakoulututkinto"
                                                     :sv "I Finland avlagd högskoleexamen"
                                                     :en "EN: Suomessa suoritettu korkeakoulututkinto"}
   :pohjakoulutus_kk_ulk                            {:fi "Muualla kuin Suomessa suoritettu korkeakoulututkinto"
                                                     :sv "Högskoleexamen som avlagt annanstans än i Finland"
                                                     :en "EN: Muualla kuin Suomessa suoritettu korkeakoulututkinto"}
   :pohjakoulutus_lk                                {:fi "Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"
                                                     :sv "I Finland avlagd gymnasiets lärokurs utan studentexamen"
                                                     :en "EN: Suomessa suoritettu lukion oppimäärä ilman ylioppilastutkintoa"}
   :pohjakoulutus_muu                               {:fi "Muu korkeakoulukelpoisuus"
                                                     :sv "Annan högskolebehörighet"
                                                     :en "EN: Muu korkeakoulukelpoisuus"}
   :pohjakoulutus_ulk                               {:fi "Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"
                                                     :sv "Annan examen som avlagts annanstans än i Finland och som i ifrågavarande land ger ansökningsbehörighet för högskola"
                                                     :en "EN: Muualla kuin Suomessa suoritettu muu tutkinto, joka asianomaisessa maassa antaa hakukelpoisuuden korkeakouluun"}
   :pohjakoulutus_yo                                {:fi "Suomessa suoritettu ylioppilastutkinto"
                                                     :sv "I Finland avlagd studentexamen"
                                                     :en "EN: Suomessa suoritettu ylioppilastutkinto"}
   :pohjakoulutus_yo_ammatillinen                   {:fi "Ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"
                                                     :sv "Yrkesinriktad grundexamen och studentexamen (dubbelexamen)"
                                                     :en "EN: Ammatillinen perustutkinto ja ylioppilastutkinto (kaksoistutkinto)"}
   :pohjakoulutus_yo_kansainvalinen_suomessa        {:fi "Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                     :sv "I Finland avlagd internationell studentexamen"
                                                     :en "EN: Suomessa suoritettu kansainvälinen ylioppilastutkinto"}
   :pohjakoulutus_yo_ulkomainen                     {:fi "Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"
                                                     :sv "Internationell studentexamen som avlagts annanstans än i Finland"
                                                     :en "EN: Muualla kuin Suomessa suoritettu kansainvälinen ylioppilastutkinto"}
   :pohjakoulutusristiriita                         {:fi "Pohjakoulutusristiriita"
                                                     :sv "Motstridighet i grundutbildningen"
                                                     :en "EN: Pohjakoulutusristiriita"}
   :points                                          {:fi "Pisteet"
                                                     :sv "Poäng"
                                                     :en "Points"}
   :processed-haut                                  {:fi "Käsitellyt haut"
                                                     :sv "Behandlade ansökningar"
                                                     :en "Processed admissions"}
   :processing-state                                {:fi "Käsittelyvaihe"
                                                     :sv "Behandlingsskede"
                                                     :en "State of processing"}
   :question                                        {:fi "Kysymys"
                                                     :sv "Fråga"
                                                     :en "Question"}
   :question-group                                  {:fi "Kysymysryhmä"
                                                     :sv "Frågegrupp"
                                                     :en "Question group"}
   :receiver                                        {:fi "Vastaanottaja:"
                                                     :sv "Mottagare:"
                                                     :en "Receiver:"}
   :rejection-reason                                {:fi "Hylkäyksen syy"
                                                     :sv "Orsak till avslag"
                                                     :en "Reason for rejection"}
   :remove                                          {:fi "Poista"
                                                     :sv "Radera"
                                                     :en "Delete"}
   :remove-filters                                  {:fi "Poista rajaimet"
                                                     :sv "Ta bort avgränsningar"
                                                     :en "Remove the filters"}
   :remove-lock                                     {:fi "Poista lukitus"
                                                     :sv "Öppna låset"
                                                     :en "Unlock the form"}
   :selection-limit                                 {:fi "Rajoitettu valinta"
                                                     :sv "Begränsat urval"
                                                     :en "Limited selection"}
   :selection-limit-input                           {:fi "Raja-arvo"
                                                     :sv "Gränsvärde"
                                                     :en "Limit"}
   :required                                        {:fi "Pakollinen tieto"
                                                     :sv "Obligatorisk uppgift"
                                                     :en "Mandatory information"}
   :per-hakukohde                                   {:fi "Vastaukset ovat hakukohdekohtaisia"
                                                     :sv "Svaren ges enligt ansökningsmål"
                                                     :en "Answers are specific to application options"}
   :invalid-date-format                             {:fi "Päivämäärän tulee olla muotoa p.k.vvvv"
                                                     :sv "Ange datumet i formen p.k.vvvv"
                                                     :en "EN: Päivämäärän tulee olla muotoa d.m.yyyy"}
   :invalid-time-format                             {:fi "Ajan tulee olla muotoa t.mm"
                                                     :sv "Ange tiden i formen h.mm"
                                                     :en "EN: Ajan tulee olla muotoa h.mm"}
   :reset-organization                              {:fi "Palauta oletusorganisaatio"
                                                     :sv "Återställ utgångsorganisation"
                                                     :en "Reset the organization"}
   :save                                            {:fi "Tallenna"
                                                     :sv "Spara"
                                                     :en "Save"}
   :save-changes                                    {:fi "Tallenna muutokset"
                                                     :sv "Spara ändringar"
                                                     :en "Save the changes"}
   :search-by-applicant-info                        {:fi "Etsi hakijan henkilötiedoilla"
                                                     :sv "Sök med sökandes personuppgifter"
                                                     :en "Search by applicant's personal information"}
   :search-sub-organizations                        {:fi "Etsi aliorganisaatioita"
                                                     :sv "Sök underorganisationer"
                                                     :en "EN: Etsi aliorganisaatioita"}
   :search-terms-list                               {:fi "Nimi, henkilötunnus, syntymäaika, sähköpostiosoite tai oidit"
                                                     :sv "Namn, personbeteckning, födelsetid eller e-postadress"
                                                     :en "Name, Finnish personal identity number, date of birth, email or OIDs"}
   :id-in-shared-use                                {:fi "(tunniste on jaetussa käytössä)"
                                                     :sv "(identifikationen är delad)"
                                                     :en "(tunniste on jaetussa käytössä)"}
   :questions                                       {:fi "kysymykset"
                                                     :sv "frågor"
                                                     :en "questions"}
   :selection                                       {:fi "Valinta"
                                                     :sv "Antagning"
                                                     :en "Selection"}
   :send-confirmation-email-to-applicant            {:fi "Lähetä vahvistussähköposti hakijalle"
                                                     :sv "Skicka e-post med bekräftelse till sökanden"
                                                     :en "Send confirmation email again"}
   :send-edit-link-to-applicant                     {:fi "Vahvistussähköposti lähetetty uudelleen hakijalle"
                                                     :sv "Bearbetningslänken har skickats till sökande per e-post"
                                                     :en "Confirmation email has been sent"}
   :send-information-request                        {:fi "Lähetä täydennyspyyntö"
                                                     :sv "Skicka begäran om komplettering"
                                                     :en "Send information request"}
   :send-information-request-to-applicant           {:fi "Lähetä täydennyspyyntö hakijalle"
                                                     :sv "Skicka begäran om komplettering till sökanden"
                                                     :en "Send information request message"}
   :sending-information-request                     {:fi "Täydennyspyyntöä lähetetään"
                                                     :sv "Begäran om komplettering skickas"
                                                     :en "Message has been sent"}
   :set-haku-to-form                                {:fi "Aseta ensin lomake haun käyttöön niin voit tehdä hakukohteen mukaan näkyviä sisältöjä."
                                                     :sv "Ställ först blanketten för användning i ansökan för att kunna bilda innehåll för ansökningsmålet."
                                                     :en "EN: Aseta ensin lomake haun käyttöön niin voit tehdä hakukohteen mukaan näkyviä sisältöjä."}
   :state                                           {:fi "Tila"
                                                     :sv "Status"
                                                     :en "EN: Tila"}
   :states-selected                                 {:fi "tilaa valittu"
                                                     :sv "status har valts"
                                                     :en "EN: tilaa valittu"}
   :liitepyynto-deadline                            {:fi "Hakijakohtainen aikaraja"
                                                     :sv "Tidsgräns enligt sökande"
                                                     :en "EN: Hakijakohtainen aikaraja"}
   :liitepyynto-deadline-date                       {:fi "Viimeinen palautusajankohta"
                                                     :sv "Sista returdatum"
                                                     :en "EN: Viimeinen palautusajankohta"}
   :liitepyynto-deadline-time                       {:fi "klo"
                                                     :sv "kl."
                                                     :en "EN: klo"}
   :liitepyynto-deadline-error                      {:fi "Aikarajan tallennus epäonnistui"
                                                     :sv "Att spara tidsgränsen misslyckades"
                                                     :en "EN: Aikarajan tallennus epäonnistui"}
   :liitepyynto-deadline-set                        {:fi "Hakijakohtainen aikaraja asetettu"
                                                     :sv "En tidsgräns enligt sökande har angetts"
                                                     :en "EN: Hakijakohtainen aikaraja asetettu"}
   :liitepyynto-deadline-unset                      {:fi "Hakijakohtainen aikaraja poistettu"
                                                     :sv "En tidsgräns enligt sökande har raderats"
                                                     :en "EN: Hakijakohtainen aikaraja poistettu"}
   :settings                                        {:fi "Asetukset"
                                                     :sv "Inställningar"
                                                     :en "EN: Asetukset"}
   :shape                                           {:fi "Muoto:"
                                                     :sv "Form:"
                                                     :en "EN: Muoto:"}
   :show-more                                       {:fi "Näytä lisää.."
                                                     :sv "Visa mer.."
                                                     :en "EN: Näytä lisää.."}
   :show-options                                    {:fi "Näytä vastausvaihtoehdot"
                                                     :sv "Visa svarsalternativ"
                                                     :en "EN: Näytä vastausvaihtoehdot"}
   :single-choice-button                            {:fi "Painikkeet, yksi valittavissa"
                                                     :sv "En tangent kan väljas"
                                                     :en "EN: Painikkeet, yksi valittavissa"}
   :single-choice-button-koodisto                   {:fi "Painikkeet, yksi valittavissa, koodisto"
                                                     :sv "En tangent kan väljas, kodregister"
                                                     :en "EN: Painikkeet, yksi valittavissa, codes"}
   :ssn                                             {:fi "Henkilötunnus"
                                                     :sv "Personbeteckning"
                                                     :en "Personal identity code"}
   :with-ssn                                        {:fi "Henkilötunnuksellinen"
                                                     :sv "Med personbeteckning"
                                                     :en "With personal identity code"}
   :without-ssn                                     {:fi "Henkilötunnukseton"
                                                     :sv "Utan personbeteckning"
                                                     :en "Without personal identity code"}
   :student                                         {:fi "Oppija"
                                                     :sv "Studerande"
                                                     :en "Applicant"}
   :submitted-application                           {:fi "syötti hakemuksen"
                                                     :sv "matade in ansökan"
                                                     :en "submitted"}
   :submitted-at                                    {:fi "Hakemus jätetty"
                                                     :sv "Ansökan inlämnad"
                                                     :en "Application submitted"}
   :swedish                                         {:fi "Ruotsi"
                                                     :sv "Svenska"
                                                     :en "Swedish"}
   :test-application                                {:fi "Testihakemus / Virkailijatäyttö"
                                                     :sv "Testansökan / Administratören fyller i"
                                                     :en "Test application"}
   :text                                            {:fi "Teksti"
                                                     :sv "Text"
                                                     :en "Text"}
   :text-area                                       {:fi "Tekstialue"
                                                     :sv "Textområde"
                                                     :en "Text area"}
   :text-area-size                                  {:fi "Tekstialueen koko"
                                                     :sv "Textområdets storlek"
                                                     :en "EN: Tekstialueen koko"}
   :text-field                                      {:fi "Tekstikenttä"
                                                     :sv "Textfält"
                                                     :en "Text field"}
   :text-field-size                                 {:fi "Tekstikentän koko"
                                                     :sv "Textfältets storlek"
                                                     :en "EN: Tekstikentän koko"}
   :title                                           {:fi "Otsikko"
                                                     :sv "Rubrik"
                                                     :en "Title"}
   :to-state                                        {:fi "Muutetaan tilaan"
                                                     :sv "Status ändras till"
                                                     :en "Change status to"}
   :unidentified                                    {:fi "Yksilöimättömät"
                                                     :sv "Inte identifierade"
                                                     :en "Unidentified"}
   :unknown                                         {:fi "Tuntematon"
                                                     :sv "Okänd"
                                                     :en "Unknown"}
   :unknown-virkailija                              {:fi "Tuntematon virkailija"
                                                     :sv "Okänd administratör"
                                                     :en "Unknown official"}
   :unknown-option                                  {:fi "Tuntematon vastausvaihtoehto"
                                                     :sv "Okänt svarsalternativ"
                                                     :en "Unknown option"}
   :unprocessed                                     {:fi "Käsittelemättä"
                                                     :sv "Obehandlad"
                                                     :en "Unprocessed"}
   :unprocessed-haut                                {:fi "Käsittelemättä olevat haut"
                                                     :sv "Obehandlade ansökningar"
                                                     :en "Unprocessed admissions"}
   :used-by-haku                                    {:fi "Tämä lomake on haun käytössä"
                                                     :sv "Denna blankett används i ansökan"
                                                     :en "EN: Tämä lomake on haun käytössä"}
   :used-by-haut                                    {:fi "Tämä lomake on seuraavien hakujen käytössä"
                                                     :sv "Denna blankett används i följande ansökningar"
                                                     :en "EN: Tämä lomake on seuraavien hakujen käytössä"}
   :kevyt-valinta-valinnan-tila-change              {:fi "Valinta: %s"
                                                     :sv "Antagning: %s"
                                                     :en "Student selection: %s"}
   :valintatuloksen-julkaisulupa                    {:fi "Valintatuloksen julkaisulupa"
                                                     :sv "Tillstånd att publicera antagningsresultat"
                                                     :en "EN : Valintatuloksen julkaisulupa"}
   :view-applications-rights-panel                  {:fi "Hakemusten katselu"
                                                     :sv "Granskning av ansökningar"
                                                     :en "EN: Hakemusten katselu"}
   :view-valinta-rights-panel                       {:fi "Valinnan tuloksen katselu"
                                                     :sv "Granskning av antagningsresultat"
                                                     :en "EN: Valinnan tuloksen katselu"}
   :virus-found                                     {:fi "Virus löytyi"
                                                     :sv "Ett virus hittades"
                                                     :en "Virus found"}
   :virus-scan-failed                               {:fi "Virustarkistus epäonnistui teknisen virheen vuoksi"
                                                     :sv "Viruskontrollen misslyckades på grund av ett tekniskt fel"
                                                     :en "Virus scan failed due to a technical error"}
   :visibility-on-form                              {:fi "Näkyvyys lomakkeella:"
                                                     :sv "Visas på blanketten:"
                                                     :en "EN: Näkyvyys lomakkeella:"}
   :visible-to-all                                  {:fi "näkyy kaikille"
                                                     :sv "visas för alla"
                                                     :en "EN: näkyy kaikille"}
   :hidden                                          {:fi "piilotettu"
                                                     :sv "dold"
                                                     :en "hidden"}
   :is-hidden?                                      {:fi "ei näytetä lomakkeella (piilotettu)"
                                                     :sv "visas inte på blanketten (dold)"
                                                     :en "EN: ei näytetä lomakkeella (piilotettu)"}
   :visible-to-hakukohteet                          {:fi "vain valituille hakukohteille",
                                                     :sv "endast för valda ansökningsmål"
                                                     :en "EN: vain valituille hakukohteille"}
   :wrapper-element                                 {:fi "Lomakeosio"
                                                     :sv "Blankettdel"
                                                     :en "Form element"}
   :wrapper-header                                  {:fi "Osion nimi"
                                                     :sv "Delens namn"
                                                     :en "Element's header"}
   :active-status                                   {:fi "Aktiivisuus"
                                                     :sv "Aktivitet"
                                                     :en "Active"}
   :active-status-active                            {:fi "Aktiiviset"
                                                     :sv "Aktiva"
                                                     :en "Actives"}
   :active-status-passive                           {:fi "Passivoidut"
                                                     :sv "Passiverade"
                                                     :en "Passives"}
   :application-count-unprocessed                   {:fi "Käsittelemättä"
                                                     :sv "Obehandlad"
                                                     :en "Unprocessed"}
   :application-count-processing                    {:fi "Käsittely on kesken"
                                                     :sv "Behandlingen är inte färdig"
                                                     :en "In process"}
   :application-count-processed                     {:fi "Käsitelty"
                                                     :sv "Behandlad"
                                                     :en "Processed"}
   :navigate-applications-forward                   {:fi "Seuraava hakemus"
                                                     :sv "Följande ansökan"
                                                     :en "Next application"}
   :navigate-applications-back                      {:fi "Edellinen hakemus"
                                                     :sv "Föregående ansökan"
                                                     :en "Previous application"}
   :autosave-enabled                                {:fi "Automaattitalletus: päällä"
                                                     :sv "Automatspar: på"
                                                     :en "Auto-save: enabled"}
   :multiple-values                                 {:fi "Monta arvoa"
                                                     :sv "Multipla värden"
                                                     :en "Multiple values"}
   :autosave-disabled                               {:fi "Automaattitalletus: pois päältä"
                                                     :sv "Automatspar: av"
                                                     :en "Auto-save: disabled"}
   :hylatty                                         {:fi "Hylätty"
                                                     :sv "Underkänd"
                                                     :en "Rejected"}
   :varalla                                         {:fi "Varalla"
                                                     :sv "På reserv"
                                                     :en "On reserve place"}
   :peruuntunut                                     {:fi "Peruuntunut"}
   :varasijalta-hyvaksytty                          {:fi "Varasijalta hyväksytty"}
   :hyvaksytty                                      {:fi "Hyväksytty"
                                                     :sv "Godkänd"
                                                     :en "Selected"}
   :julkaistu                                       {:fi "Julkaistu"}
   :ei-julkaistu                                    {:fi "Ei julkaistu"}
   :ei-vastaanotettu-maaraaikana                    {:fi "Ei vastaanotettu määräaikana"}
   :perunut                                         {:fi "Perunut"}
   :peruutettu                                      {:fi "Peruutettu"}
   :ottanut-vastaan-toisen-paikan                   {:fi "Ottanut vastaan toisen paikan"}
   :ehdollisesti-vastaanottanut                     {:fi "Ehdollisesti vastaanottanut"}
   :vastaanottanut-sitovasti                        {:fi "Vastaanottanut sitovasti"}
   :kesken                                          {:fi "Kesken"
                                                     :sv "Inte färdig"
                                                     :en "Incomplete"}
   :vastaanottanut                                  {:fi "Vastaanottanut"}
   :ei-tehty                                        {:fi "Ei tehty"}
   :lasna-koko-lukuvuosi                            {:fi "Läsnä (koko lukuvuosi)"}
   :poissa-koko-lukuvuosi                           {:fi "Poissa (koko lukuvuosi)"}
   :ei-ilmoittautunut-maaraaikana                   {:fi "Ei ilmoittautunut määräaikana"}
   :lasna-syksy                                     {:fi "Läsnä syksy, poissa kevät"}
   :poissa-syksy                                    {:fi "Poissa syksy, läsnä kevät"}
   :lasna                                           {:fi "Läsnä, keväällä alkava koulutus"}
   :poissa                                          {:fi "Poissa, keväällä alkava koulutus"}
   :valinta                                         {:fi "Valinta"
                                                     :sv "Antagning"
                                                     :en "Selection"}
   :julkaisu                                        {:fi "Julkaisu"}
   :vastaanotto                                     {:fi "Vastaanotto"}
   :ilmoittautuminen                                {:fi "Ilmoittautuminen"}
   :odottamaton-virhe-otsikko                       {:fi "Tapahtui odottamaton virhe"
                                                     :sv "Ett oväntat fel uppstod"}
   :odottamaton-virhe-aputeksti                     {:fi "Yritä uudelleen tai ota yhteyttä ylläpitoon."
                                                     :sv "Försök igen, om problemet kvarstår, kontakta registratorn."}
   :cannot-deactivate-info                          {:fi "Hakemuksen tilaa ei voi muuttaa, koska hakemukselle on muodostunut valinnan tuloksia. Ota yhteyttä Opetushallitukseen hakemuksen passivoimiseksi."
                                                     :sv "Ansökningens status kan inte ändras eftersom antagningsresultat har bildats för ansökningen. Kontakta Utbildningsstyrelsen för att få ansökningen passiverad."
                                                     :en "EN: Hakemuksen tilaa ei voi muuttaa, koska hakemukselle on muodostunut valinnan tuloksia. Ota yhteyttä Opetushallitukseen hakemuksen passivoimiseksi."}
   :lisakysymys                                     {:fi "Lisäkysymys"
                                                     :sv "Tilläggsfråga"
                                                     :en "Extra question"}
   :lisakysymys-arvon-perusteella                   {:fi "Lisäkysymys arvon perusteella"
                                                     :sv "SV: Lisäkysymys arvon perusteella"
                                                     :en "EN: Lisäkysymys arvon perusteella"}
   :lisakysymys-arvon-perusteella-ehto              {:fi "Jos vastauksen arvo on"
                                                     :sv "Om svarets värde är"
                                                     :en "EN: Jos vastauksen arvo on"}
   :lisakysymys-arvon-perusteella-ehto-pienempi     {:fi "pienempi kuin"
                                                     :sv "mindre än"
                                                     :en "EN: less than"}
   :lisakysymys-arvon-perusteella-ehto-suurempi     {:fi "suurempi kuin"
                                                     :sv "större än"
                                                     :en "EN: greater than"}
   :lisakysymys-arvon-perusteella-ehto-yhtasuuri    {:fi "yhtä suuri kuin"
                                                     :sv "lika med"
                                                     :en "EN: equal to"}
   :lisakysymys-arvon-perusteella-lisaa-ehto        {:fi "Lisää ehto"
                                                     :sv "Lisää ehto"
                                                     :en "EN: Lisää ehto"}
   :filter-by-question-answer                       {:fi "Rajaa vastauksen mukaan"
                                                     :sv "Avgränsa enligt svar"
                                                     :en "EN: Rajaa vastauksen mukaan"}
   :question-answer                                 {:fi "Vastaus"
                                                     :sv "Svar"
                                                     :en "EN: Vastaus"}
   :question-answers-selected                       {:fi "vastausvaihtoehtoa valittu"
                                                     :sv "Svarsalternativ valt"
                                                     :en "EN: vastausvaihtoehtoa valittu"}})

(def state-translations
  {:active                 {:fi "Aktiivinen"
                            :sv "Aktiv"
                            :en "Active"}
   :passive                {:fi "Passiivinen"
                            :sv "Passiv"
                            :en "Inactive"}
   :unprocessed            {:fi "Käsittelemättä"
                            :sv "Obehandlad"
                            :en "Unprocessed"}
   :processing             {:fi "Käsittelyssä"
                            :sv "Under behandling"
                            :en "Under process"}
   :invited-to-interview   {:fi "Kutsuttu haast."
                            :sv "Kallad till intervju"
                            :en "Invited to interview"}
   :invited-to-exam        {:fi "Kutsuttu valintak."
                            :sv "Kallad till urvalsprov"
                            :en "Invited to entrance examination"}
   :evaluating             {:fi "Arvioinnissa"
                            :sv "Under bedömning"
                            :en "Under evaluation"}
   :valintaesitys          {:fi "Valintaesitys"
                            :sv "Antagningsförslag"
                            :en "EN: Valintaesitys"}
   :processed              {:fi "Käsitelty"
                            :sv "Behandlad"
                            :en "Processed"}
   :information-request    {:fi "Täydennyspyyntö"
                            :sv "Begäran om komplettering"
                            :en "Information request"}
   :incomplete             {:fi "Kesken"
                            :sv "Inte färdig"
                            :en "Incomplete"}
   :selection-proposal     {:fi "Valintaesitys"
                            :sv "Antagningsförslag"
                            :en "Selected (pending)"}
   :reserve                {:fi "Varalla"
                            :sv "På reserv"
                            :en "On reserve place"}
   :selected               {:fi "Hyväksytty"
                            :sv "Godkänd"
                            :en "Selected"}
   :rejected               {:fi "Hylätty"
                            :sv "Underkänd"
                            :en "Rejected"}
   :unreviewed             {:fi "Tarkastamatta"
                            :sv "Inte granskad"
                            :en "Unreviewed"}
   :fulfilled              {:fi "Täyttyy"
                            :sv "Fylls"
                            :en "Fills"}
   :unfulfilled            {:fi "Ei täyty"
                            :sv "Fylls inte"
                            :en "Not filling"}
   :eligible               {:fi "Hakukelpoinen"
                            :sv "Ansökningsbehörig"
                            :en "Eligible"}
   :uneligible             {:fi "Ei hakukelpoinen"
                            :sv "Inte ansökningsbehörig"
                            :en "Not eligible"}
   :conditionally-eligible {:fi "Ehdollisesti hakukelpoinen"
                            :sv "SV: Ehdollisesti hakukelpoinen"
                            :en "Conditionally eligible"}
   :obligated              {:fi "Velvollinen"
                            :sv "Förpliktad"
                            :en "Obligated"}
   :not-obligated          {:fi "Ei velvollinen"
                            :sv "Inte förpliktad"
                            :en "Not obligated"}
   :processing-state       {:fi "Käsittelyvaihe"
                            :sv "Behandlingsskede"
                            :en "State of processing"}
   :language-requirement   {:fi "Kielitaitovaatimus"
                            :sv "Språkkunskapskrav"
                            :en "Language requirment"}
   :degree-requirement     {:fi "Tutkinnon kelpoisuus"
                            :sv "Examens behörighet"
                            :en "Degree requirement"}
   :eligibility-state      {:fi "Hakukelpoisuus"
                            :sv "Ansökningsbehörighet"
                            :en "Eligibility"}
   :payment-obligation     {:fi "Maksuvelvollisuus"
                            :sv "Betalningsskyldighet"
                            :en "Obligated to pay"}
   :selection-state        {:fi "Valinta"
                            :sv "Antagning"
                            :en "Selection"}
   :not-checked            {:fi "Tarkastamatta"
                            :sv "Inte granskad"
                            :en "Not checked"}
   :checked                {:fi "Tarkistettu"
                            :sv "Granskad"
                            :en "Checked"}
   :incomplete-answer      {:fi "Puutteellinen"
                            :sv "Bristfällig"
                            :en "Incomplete"}
   :overdue                {:fi "Myöhässä"
                            :sv "Försenad"
                            :en "Overdue"}
   :no-attachment-required {:fi "Ei liitepyyntöä"
                            :sv "Ei liitepyyntöä (sv) TODO"
                            :en "Ei liitepyyntöä (en) TODO"}
   :incomplete-attachment  {:fi "Puutteellinen liite"
                            :sv "Puutteellinen liite (sv) TODO"
                            :en "Puutteellinen liite (en) TODO"}
   :attachment-missing     {:fi "Liite puuttuu"
                            :sv "Liite puuttuu (sv) TODO"
                            :en "Liite puuttuu (en) TODO"}
   :multiple-values        {:fi "Monta arvoa"
                            :sv "Multipla värden"
                            :en "Multiple values"}})


(def excel-texts
  {:name                     {:fi "Nimi"
                              :sv "Namn"
                              :en "EN: Nimi"}
   :id                       {:fi "Id"
                              :sv "Id"
                              :en "EN: Id"}
   :key                      {:fi "Tunniste"
                              :sv "Identifikation"
                              :en "EN: Tunniste"}
   :created-time             {:fi "Viimeksi muokattu"
                              :sv "Senast bearbetad"
                              :en "EN: Viimeksi muokattu"}
   :created-by               {:fi "Viimeinen muokkaaja"
                              :sv "Senast bearbetad av"
                              :en "EN: Viimeinen muokkaaja"}
   :sent-at                  {:fi "Lähetysaika"
                              :sv "Sändningstid"
                              :en "EN: Lähetysaika"}
   :application-state        {:fi "Hakemuksen tila"
                              :sv "Ansökans status"
                              :en "EN: Hakemuksen tila"}
   :hakukohde-handling-state {:fi "Hakukohteen käsittelyn tila"
                              :sv "Status för behandling av ansökningsmålet"
                              :en "EN: Hakukohteen käsittelyn tila"}
   :kielitaitovaatimus       {:fi "Kielitaitovaatimus"
                              :sv "Språkkunskapskrav"
                              :en "EN: Kielitaitovaatimus"}
   :tutkinnon-kelpoisuus     {:fi "Tutkinnon kelpoisuus"
                              :sv "Examens behörighet"
                              :en "EN: Tutkinnon kelpoisuus"}
   :hakukelpoisuus           {:fi "Hakukelpoisuus"
                              :sv "Ansökningsbehörighet"
                              :en "EN: Hakukelpoisuus"}
   :maksuvelvollisuus        {:fi "Maksuvelvollisuus"
                              :sv "Betalningsskyldighet"
                              :en "EN: Maksuvelvollisuus"}
   :valinnan-tila            {:fi "Valinnan tila"
                              :sv "Antagningens status"
                              :en "EN: Valinnan tila"}
   :ehdollinen               {:fi "Ehdollinen"
                              :sv "SV: Ehdollinen"
                              :en "EN: Ehdollinen"}
   :pisteet                  {:fi "Pisteet"
                              :sv "Poäng"
                              :en "EN: Pisteet"}
   :applicant-oid            {:fi "Hakijan henkilö-OID"
                              :sv "Sökandes person-OID"
                              :en "EN: Hakijan henkilö-OID"}
   :turvakielto              {:fi "Turvakielto"
                              :sv "Spärrmarkering"
                              :en "EN: Turvakielto"}
   :notes                    {:fi "Muistiinpanot"
                              :sv "Anteckningar"
                              :en "EN: Muistiinpanot"}})

(defn email-applied-error
  [email preferred-name]
  {:fi [:div
        [:p (if (not (string/blank? preferred-name))
              (str "Hei " preferred-name "!")
              "Hei!")]
        [:p "Huomasimme, että "
         [:strong "olet jo lähettänyt hakemuksen"]
         " tähän hakuun ja siksi et voi lähettää toista hakemusta."]
        [:p "Jos haluat "
         [:strong "muuttaa hakemustasi"]
         " niin löydät muokkauslinkin sähköpostiviestistä jonka sait
         jättäessäsi edellisen hakemuksen."]
        [:p "Tarkista myös, että syöttämäsi sähköpostiosoite "
         [:strong email]
         " on varmasti oikein."]
        [:p "Ongelmatilanteissa ole yhteydessä oppilaitokseen johon haet."]]
   :sv [:div
        [:p (if (not (string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "Vi märkte att "
         [:strong "du redan har skickat en ansökning"]
         " i denna ansökan och därför kan du inte skicka en annan
          ansökning."]
        [:p "Om du vill "
         [:strong "ändra din ansökning"]
         " hittar du bearbetningslänken i e-postmeddelandet som du fick när
          du skickade din tidigare ansökning."]
        [:p "Kontrollera även att e-postadressen du har angett "
         [:strong email]
         " säkert är korrekt."]
        [:p "Vid eventuella problemsituationer kontakta den läroanstalt du
             söker till."]]
   :en [:div
        [:p (if (not (string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "we noticed that "
         [:strong "you have already submitted an application"]
         " to this admission and therefore cannot submit another
          application. If you submit several applications, only the latest one
          will be taken into consideration and all others will be discarded."]
        [:p "If you want to "
         [:strong "make changes"]
         " to your previous application, you can do so by clicking the link
          in the confirmation email you have received with your earlier
          application."]
        [:p "Please also check that the email address "
         [:strong email]
         " you have given is correct."]
        [:p "If you have any problems, please contact the educational
             institution you are applying to."]]})

(defn email-applied-error-when-modifying
  [email preferred-name]
  {:fi [:div
        [:p (if (not (string/blank? preferred-name))
              (str "Hei " preferred-name "!")
              "Hei!")]
        [:p "Antamallasi sähköpostiosoitteella "
         [:strong email]
         " on jo jätetty hakemus. Tarkista, että syöttämäsi sähköpostiosoite
          on varmasti oikein."]]
   :sv [:div
        [:p (if (not (string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "En ansökning med den e-postadress du angett "
         [:strong email]
         " har redan gjorts. Kontrollera att e-postadressen du har angett
          säkert är korrekt."]]
   :en [:div
        [:p (if (not (string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "the email address "
         [:strong email]
         " you have given in your application has already been used by
          another applicant. Please check that the email address you have
          given is correct."]]})

(defn ssn-applied-error
  [preferred-name]
  {:fi [:div
        [:p (if (not (string/blank? preferred-name))
              (str "Hei " preferred-name "!")
              "Hei!")]
        [:p "Huomasimme, että "
         [:strong "olet jo lähettänyt hakemuksen"]
         " tähän hakuun ja siksi et voi lähettää toista hakemusta."]
        [:p "Jos haluat "
         [:strong "muuttaa hakemustasi"]
         " niin löydät muokkauslinkin sähköpostiviestistä jonka sait
           jättäessäsi edellisen hakemuksen."]
        [:p "Ongelmatilanteissa ole yhteydessä hakemaasi oppilaitokseen."]]
   :sv [:div
        [:p (if (not (string/blank? preferred-name))
              (str "Hej " preferred-name "!")
              "Hej!")]
        [:p "Vi märkte att "
         [:strong "du redan har skickat en ansökning"]
         " i denna ansökan och därför kan du inte skicka en annan
           ansökning."]
        [:p "Om du vill "
         [:strong "ändra din ansökning"]
         " hittar du bearbetningslänken i e-postmeddelandet som du fick när
           du skickade din tidigare ansökning."]
        [:p "Vid eventuella problemsituationer kontakta den läroanstalt du
         söker till."]]
   :en [:div
        [:p (if (not (string/blank? preferred-name))
              (str "Dear " preferred-name ",")
              "Dear applicant,")]
        [:p "we noticed that "
         [:strong "you have already submitted an application"]
         " to this admission. Therefore, you cannot submit another
          application to the same admission."]
        [:p "If you want to "
         [:strong "make changes"]
         " to your previous application, you can do so, by clicking the link
          in the confirmation email you have received with your earlier
          application."]
        [:p "If you have any problems, please contact the educational
         institution."]]})
