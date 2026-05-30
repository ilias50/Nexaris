import en from './en'

const de = {
  ...en,
  nav: {
    ...en.nav,
    profile: 'Mein Profil',
    language: 'Sprache',
    logout: 'Abmelden',
  },
  locale: {
    fr: 'Franzosisch',
    en: 'Englisch',
    nl: 'Niederlandisch',
    de: 'Deutsch',
  },
  login: {
    ...en.login,
    subtitle: 'Melden Sie sich in Ihrem Arbeitsbereich an',
    submit: 'Anmelden',
  },
  register: {
    ...en.register,
    subtitle: 'Erstellen Sie Ihr Nexaris-Konto',
    submit: 'Konto erstellen',
    backToLogin: 'Zuruck zur Anmeldung',
  },
}

export default de
