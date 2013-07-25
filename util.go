package main
import (
	"github.com/gorilla/sessions"
	"code.google.com/p/goauth2/oauth"
	"encoding/json"
	"fmt"
	"net/http"
)

// Cookie store used to store the user's ID in the current session.
var store = sessions.NewCookieStore([]byte(secret))

// OAuth2.0 configuration variables.
func config(host string) *oauth.Config {
	r := &oauth.Config{
		ClientId:       clientId,
		ClientSecret:   clientSecret,
		Scope:          scopes,
		AuthURL:        "https://accounts.google.com/o/oauth2/auth",
		TokenURL:       "https://accounts.google.com/o/oauth2/token",
		AccessType:     "offline",
		ApprovalPrompt: "force",
	}
	if len(host) > 0 {
		r.RedirectURL = fullUrl + "/oauth2callback"
	}
	return r
}

func storeUserID(w http.ResponseWriter, r *http.Request, userId string) error {
	session, err := store.Get(r, sessionName)
	if err != nil {
		fmt.Println("Couldn't get sessionName")
		return err
	}
	fmt.Println("Saves session")
	session.Values["userId"] = userId
	return session.Save(r, w)
}

// userID retrieves the current user's ID from the session's cookies.
func userID(r *http.Request) (string, error) {
	session, err := store.Get(r, sessionName)
	if err != nil {
		return "", err
	}
	userId := session.Values["userId"]
	if userId != nil {
		fmt.Println("Got session: " + userId.(string))
		return userId.(string), nil
	}
	return "", nil
}

func storeCredential(userId string, token *oauth.Token) error {
	// Store the tokens in the datastore.
	val, err := json.Marshal(token)
	if err != nil {
		return err
	}
	err = setUserAttribute(userId, "oauth_token", string(val))
	return err
}

func authTransport(userId string) *oauth.Transport {
	val, err := getUserAttribute(userId, "oauth_token")
	if err != nil {
		return nil
	}
	tok := new(oauth.Token)
	err = json.Unmarshal([]byte(val), tok)
	if err != nil {
		return nil
	}
	return &oauth.Transport{
		Config:    config(""),
		Token:     tok,
	}
}

func deleteCredential(userId string) error {
	return deleteUserAttribute(userId, "oauth_token")
}
