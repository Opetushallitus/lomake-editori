(function() {
  function closedFormList() {
    return testFrame().find('.application-handling__form-list-closed')
  }

  function form1OnList() {
    return testFrame().find('.application-handling__form-list-row:contains(Selaintestilomake1)')
  }

  function form2OnList() {
    return testFrame().find('.application-handling__form-list-row:contains(Selaintestilomake2)')
  }

  function downloadLink() {
    return testFrame().find('.application-handling__excel-download-link')
  }

  function closedFormListExists() {
    return elementExists(closedFormList())
  }

  function navigateToApplicationHandling() {
    loadInFrame('http://localhost:8350/lomake-editori/applications/')
  }

  afterEach(function() {
    expect(window.uiError || null).to.be.null
  })

  describe('Application handling', function() {
    describe('form 1', function() {
      // Tie these to describe-scope instead of global
      var firstNotSelected = null;
      var eventCountBefore = null;
      var firstNotSelectedCaption = null;
      before(
        navigateToApplicationHandling,
        wait.until(closedFormListExists),
        clickElement(closedFormList),
        function() {
          // clickElement doesn't work for a href, jquery's click() does:
          form1OnList().click()
        },
        wait.until(function() { return closedFormList().text() ===  'Selaintestilomake1' }),
        clickElement(function() { return testFrame().find('.application-handling__list-row:not(.application-handling__list-header)') }),
        wait.until(function() { return testFrame().find('.application-handling__review-header').length > 0 }),
        function() {
          var notSelected =  testFrame().find('.application-handling__review-state-row:not(.application-handling__review-state-selected-row)')
          expect(notSelected.length).to.be.at.least(1)
          firstNotSelected = notSelected.first()
          firstNotSelectedCaption = firstNotSelected.text()
          eventCountBefore = testFrame().find('.application-handling__event-caption').length
          expect(eventCountBefore).to.be.at.least(1)
        },
        clickElement(function () { return firstNotSelected }),
        wait.until(function() { return eventCountBefore < testFrame().find('.application-handling__event-caption').length })
      )
      it('has applications', function() {
        expect(closedFormList().text()).to.equal('Selaintestilomake1')
        expect(downloadLink().text()).to.equal('Lataa hakemukset Excel-muodossa (1)')
      })
      it('Stores an event for review state change', function() {
        expect(eventCountBefore+1).to.equal(testFrame().find('.application-handling__event-caption').length)
        var lastEventNow = testFrame().find('.application-handling__event-caption').last().text()
        expect(lastEventNow).to.equal(firstNotSelectedCaption)
      })
    })
    describe('form 2 (no applications)', function() {
      before(
        function() { closedFormList()[0].click() },
        wait.until(function() {
          return form2OnList().text() === 'Lomake: Selaintestilomake2'
        }),
        function() { form2OnList()[0].click() },
        wait.until(function() { return closedFormList().text() === 'Selaintestilomake2' })
      )
      it('has no applications', function() {
        expect(closedFormList().text()).to.equal('Selaintestilomake2')
        expect(downloadLink()).to.have.length(0)
      })
    })
  })
})();
