package com.urosdragojevic.realbookstore.controller;

import com.urosdragojevic.realbookstore.domain.*;
import com.urosdragojevic.realbookstore.repository.*;
import com.urosdragojevic.realbookstore.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class BooksController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RatingRepository ratingRepository;

    @Autowired
    private PersonRepository personRepository;

    @GetMapping({"/", "/books"})
    @PreAuthorize("hasAuthority('VIEW_BOOKS_LIST')")
    public String home(Model model) {
        model.addAttribute("books", bookRepository.getAll());
        return "books";
    }

    @GetMapping("/books/{id}")
    public String book(Model model, @PathVariable int id) {

        Book book = bookRepository.get(id);

        List<Genre> genres = genreRepository.getAllForBook(id);
        book.setGenres(genres);

        book.setRating(ratingRepository.getAll(id));
        book.setOverallRating(ratingRepository.getRatingForBook(id));

        List<Comment> comments = commentRepository.getAll(id);
        List<ViewComment> viewComments = new ArrayList<>();

        for (Comment comment : comments) {
            Person person = personRepository.get(String.valueOf(comment.getUserId()));
            viewComments.add(new ViewComment(person.getFirstName() + " " + person.getLastName(), comment.getComment()));
        }

        book.setComments(commentRepository.getAll(id));

        model.addAttribute("book", book);
        model.addAttribute("comments", viewComments);
        book.getRating()
                .stream()
                .filter(rating -> rating.getUserId() == 1)
                .findFirst()
                .ifPresent(rating -> model.addAttribute("userRating", rating.getRating()));
        return "book";
    }

    //Autorizacija
    @GetMapping("/create-form")
    @PreAuthorize("hasAuthority('CREATE_BOOK')")
    public String CreateForm(Model model) throws AccessDeniedException {
        if(SecurityUtil.hasPermission("CREATE_BOOK")) {
            model.addAttribute("genres", genreRepository.getAll());
            return "create-form";
        }
        throw new AccessDeniedException("Forbidden");
    }

    @GetMapping(value = "/books/search", produces = "application/json")
    @PreAuthorize("hasAuthority('VIEW_BOOKS_LIST')")
    @ResponseBody
    public List<Book> search(@RequestParam("query") String query) throws SQLException {
        return bookRepository.search(query);
    }

    @PostMapping("/books")
    @PreAuthorize("hasAuthority('CREATE_BOOK')")
    public String createBook(NewBook book) {
        List<Genre> genreList = this.genreRepository.getAll();
        List<Genre> genresToInsert = book.getGenres().stream().map(bookGenreId -> genreList.stream().filter(genre -> genre.getId() == bookGenreId).findFirst().get()).collect(Collectors.toList());
        long id = bookRepository.create(book, genresToInsert);
        return "redirect:/books?id=" + id;
    }
}
