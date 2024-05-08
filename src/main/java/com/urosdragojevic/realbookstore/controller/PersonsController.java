package com.urosdragojevic.realbookstore.controller;

import com.urosdragojevic.realbookstore.audit.AuditLogger;
import com.urosdragojevic.realbookstore.domain.Person;
import com.urosdragojevic.realbookstore.domain.User;
import com.urosdragojevic.realbookstore.repository.PersonRepository;
import com.urosdragojevic.realbookstore.repository.UserRepository;
import com.urosdragojevic.realbookstore.security.SecurityUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.List;

@Controller
public class PersonsController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonsController.class);
    private static final AuditLogger auditLogger = AuditLogger.getAuditLogger(PersonRepository.class);

    private final PersonRepository personRepository;
    private final UserRepository userRepository;

    public PersonsController(PersonRepository personRepository, UserRepository userRepository) {
        this.personRepository = personRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/persons/{id}")
    @PreAuthorize("hasAuthority('VIEW_PERSON')")
    //CSRF
    //Autorizacija
    public String person(@PathVariable int id, Model model, HttpSession session) throws AccessDeniedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User current = (User) authentication.getPrincipal();

        // Dozvoljeno i preko persons/{id} da se pristupi kao i /myprofile
        if (id == current.getId() || SecurityUtil.hasPermission("VIEW_PERSON")) {
            String csrf = session.getAttribute("CSRF_TOKEN").toString();
            model.addAttribute("CSRF_TOKEN", session.getAttribute("CSRF_TOKEN"));
            model.addAttribute("person", personRepository.get("" + id));
            return "person";
        }

        throw new AccessDeniedException("Forbidden");
    }

    @GetMapping("/myprofile")
    @PreAuthorize("hasAuthority('VIEW_MY_PROFILE')")
    public String self(Model model, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        model.addAttribute("person", personRepository.get("" + user.getId()));
        return "person";
    }

    @DeleteMapping("/persons/{id}")
    @PreAuthorize("hasAuthority('UPDATE_PERSON')")
    public ResponseEntity<Void> person(@PathVariable int id) {
        personRepository.delete(id);
        userRepository.delete(id);

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/update-person")
    @PreAuthorize("hasAuthority('UPDATE_PERSON')")
    //CSRF
    //Autorizacija
    public String updatePerson(Person person, HttpSession session,
                               @RequestParam() String csrfToken) throws AccessDeniedException
    {
        String csrf = session.getAttribute("CSRF_TOKEN").toString();
        if (!csrf.equals(csrfToken)){
            throw new AccessDeniedException("Forbidden");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var current_user = (User) authentication.getPrincipal();
        if(Integer.valueOf(person.getId()) != current_user.getId())
            throw new AccessDeniedException("Forbidden");

        personRepository.update(person);
        return "redirect:/persons/" + person.getId();
    }

    @GetMapping("/persons")
    @PreAuthorize("hasAuthority('VEIW_PERSONS_LIST')")
    public String persons(Model model) {
        model.addAttribute("persons", personRepository.getAll());
        return "persons";
    }

    @GetMapping(value = "/persons/search", produces = "application/json")
    @PreAuthorize("hasAuthority('VIEW_PERSONS_LIST')")
    @ResponseBody
    public List<Person> searchPersons(@RequestParam String searchTerm) throws SQLException {
        return personRepository.search(searchTerm);
    }
}
