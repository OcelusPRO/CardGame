/**
 * The panel a viewer writes a card from, under the stream or over it.
 *
 * Everything here runs in the viewer's browser on Twitch's domain, so nothing it says can
 * be trusted by itself: every call carries the token Twitch signed, and the server reads
 * the channel out of that token rather than out of anything this page sends. Which is why
 * there is no channel id, no game code and no host name anywhere in this file.
 *
 * Bits are the one thing the panel really does: `useBits` hands back a receipt Twitch
 * signed, and that receipt — not the click, not the amount printed on the button — is what
 * the server prices the proposal from.
 */
;(function () {
  const API = window.CARDGAME_API
  const KINDS = { SITUATION: 'SITUATION', PUNCHLINE: 'PUNCHLINE' }
  const MAX_LENGTH = 200

  /** Everything the panel knows, in one place, because everything here redraws from it. */
  const state = {
    token: null,
    game: null,
    kind: KINDS.SITUATION,
    /** What the viewer has typed. The panel redraws itself often; a draft must survive it. */
    draft: '',
    /** The purchase we are waiting on, so a receipt lands on the card that paid for it. */
    pending: null,
    busy: false,
    notice: null,
    noticeKind: 'info',
  }

  const el = {}

  document.addEventListener('DOMContentLoaded', function () {
    el.root = document.querySelector('[data-root]')
    if (!el.root) return
    render()
    listen()
  })

  function listen() {
    if (!window.Twitch || !window.Twitch.ext) {
      return fail("Cette page doit tourner dans Twitch pour savoir de quelle chaîne il s'agit.")
    }
    Twitch.ext.onAuthorized(function (auth) {
      state.token = auth.token
      refresh()
    })
    Twitch.ext.onError(function () {
      fail('Twitch ne répond pas. Réessayez dans un instant.')
    })
    if (Twitch.ext.bits) {
      Twitch.ext.bits.onTransactionComplete(function (transaction) {
        submitPaid(transaction.transactionReceipt)
      })
      Twitch.ext.bits.onTransactionCancelled(function () {
        state.pending = null
        say("Paiement annulé, la carte n'a pas été envoyée.", 'info')
      })
    }
  }

  // --- talking to the game ------------------------------------------------------------

  function call(path, options) {
    const settings = Object.assign({ headers: {} }, options || {})
    settings.headers = Object.assign(
      { Authorization: 'Bearer ' + state.token, 'Content-Type': 'application/json' },
      settings.headers,
    )
    return fetch(API + path, settings).then(function (response) {
      return response
        .json()
        .catch(function () {
          return {}
        })
        .then(function (body) {
          if (!response.ok) throw new Error(body.detail || body.code || 'Erreur inattendue.')
          return body
        })
    })
  }

  function refresh() {
    call('/api/twitch/extension/state')
      .then(function (game) {
        state.game = game
        if (!game.situations && game.punchlines) state.kind = KINDS.PUNCHLINE
        render()
      })
      .catch(function (error) {
        fail(error.message)
      })
  }

  function send(receipt) {
    const text = state.draft.trim()
    if (text.length < 3) return say('Écrivez au moins quelques mots.', 'warn')
    state.busy = true
    render()
    call('/api/twitch/extension/cards', {
      method: 'POST',
      body: JSON.stringify({ kind: state.kind, text: text, receipt: receipt || null }),
    })
      .then(function (result) {
        state.draft = ''
        state.busy = false
        say(result.message || 'Votre carte est dans le paquet.', 'good')
        refresh()
      })
      .catch(function (error) {
        state.busy = false
        say(error.message, 'warn')
      })
  }

  /** A cheer only becomes a card once Twitch confirms the cheer actually happened. */
  function submitPaid(receipt) {
    if (!state.pending) return
    state.pending = null
    send(receipt)
  }

  function buy(sku) {
    if (!Twitch.ext.bits) return say('Les bits ne sont pas disponibles ici.', 'warn')
    if (state.draft.trim().length < 3) {
      return say("Écrivez votre carte d'abord, on paie ensuite.", 'warn')
    }
    state.pending = sku
    Twitch.ext.bits.useBits(sku)
  }

  // --- drawing ------------------------------------------------------------------------

  function render() {
    const game = state.game
    if (!game) return draw(waiting())
    if (!game.open) return draw(closed())
    draw(form(game))
  }

  function draw(nodes) {
    el.root.innerHTML = ''
    el.root.appendChild(nodes)
  }

  function waiting() {
    return card('<p class="muted">On regarde si une partie tourne…</p>')
  }

  function closed() {
    return card(
      '<h1>Sans Filtre</h1>' +
        '<p class="muted">Aucune partie ouverte aux propositions sur cette chaîne pour le moment.</p>',
    )
  }

  function form(game) {
    const node = card(
      '<h1>Écrivez une carte</h1>' +
        kindPicker(game) +
        '<textarea data-text maxlength="' +
        MAX_LENGTH +
        '" rows="3" placeholder="' +
        placeholder() +
        '"></textarea>' +
        '<p class="hint" data-hint></p>' +
        actions(game) +
        notice() +
        '<p class="muted small">' +
        game.written +
        ' carte(s) déjà écrite(s) par le tchat, sur ' +
        game.limit +
        '.</p>',
    )
    wire(node, game)
    return node
  }

  function kindPicker(game) {
    const options = []
    if (game.situations) options.push([KINDS.SITUATION, 'Une situation'])
    if (game.punchlines) options.push([KINDS.PUNCHLINE, 'Une réponse'])
    if (options.length < 2) return ''
    return (
      '<div class="picker">' +
      options
        .map(function (option) {
          const on = state.kind === option[0] ? ' class="on"' : ''
          return '<button type="button" data-kind="' + option[0] + '"' + on + '>' + option[1] + '</button>'
        })
        .join('') +
      '</div>'
    )
  }

  function actions(game) {
    if (game.access === 'CHANNEL_POINTS') {
      // A redemption never reaches an extension: it goes straight from the reward to the
      // game. All the panel can usefully do is name the reward and its price.
      const named = (game.rewards || [])
        .map(function (reward) {
          return '<li>' + escape(reward.title) + ' — <strong>' + reward.cost + '</strong> points</li>'
        })
        .join('')
      return (
        '<p class="pay">Sur cette chaîne, une carte se propose avec les <strong>points de chaîne</strong> :' +
        ' échangez la récompense sous le tchat, et écrivez votre carte dans sa case.</p>' +
        (named ? '<ul class="rewards">' + named + '</ul>' : '')
      )
    }
    if (game.access === 'BITS') {
      if (!game.products.length) {
        return '<p class="pay">Aucun tarif configuré pour l’instant.</p>'
      }
      return (
        '<div class="picker">' +
        game.products
          .map(function (product) {
            return (
              '<button type="button" data-sku="' +
              product.sku +
              '"' +
              (state.busy ? ' disabled' : '') +
              '>Envoyer pour ' +
              product.bits +
              ' bits</button>'
            )
          })
          .join('') +
        '</div>'
      )
    }
    return (
      '<button type="button" class="send" data-send' +
      (state.busy ? ' disabled' : '') +
      '>Envoyer ma carte</button>'
    )
  }

  function placeholder() {
    return state.kind === KINDS.SITUATION
      ? 'Le pire cadeau de Noël, c’est ____.'
      : 'un chat mouillé'
  }

  function notice() {
    if (!state.notice) return ''
    return '<p class="notice ' + state.noticeKind + '">' + escape(state.notice) + '</p>'
  }

  function wire(node, game) {
    el.text = node.querySelector('[data-text]')
    el.text.value = state.draft
    el.text.addEventListener('input', function () {
      // Kept in state rather than read at send time: any redraw would otherwise wipe it.
      state.draft = el.text.value
    })
    const hint = node.querySelector('[data-hint]')
    hint.textContent =
      state.kind === KINDS.SITUATION
        ? 'Les ____ marquent le trou que les joueurs rempliront.'
        : 'Une réponse courte, qui claque.'

    node.querySelectorAll('[data-kind]').forEach(function (button) {
      button.addEventListener('click', function () {
        state.kind = button.getAttribute('data-kind')
        state.notice = null
        render()
      })
    })
    const sender = node.querySelector('[data-send]')
    if (sender) {
      sender.addEventListener('click', function () {
        send(null)
      })
    }
    node.querySelectorAll('[data-sku]').forEach(function (button) {
      button.addEventListener('click', function () {
        buy(button.getAttribute('data-sku'))
      })
    })
    if (game.access !== 'CHANNEL_POINTS') {
      el.text.focus()
      el.text.setSelectionRange(state.draft.length, state.draft.length)
    }
  }

  function card(html) {
    const node = document.createElement('div')
    node.className = 'card'
    node.innerHTML = html
    return node
  }

  function say(message, kind) {
    state.notice = message
    state.noticeKind = kind || 'info'
    render()
  }

  function fail(message) {
    state.game = null
    draw(card('<h1>Sans Filtre</h1><p class="notice warn">' + escape(message) + '</p>'))
  }

  function escape(text) {
    const node = document.createElement('span')
    node.textContent = text
    return node.innerHTML
  }
})()
