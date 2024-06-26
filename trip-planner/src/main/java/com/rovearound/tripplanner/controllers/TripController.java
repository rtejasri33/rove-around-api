package com.rovearound.tripplanner.controllers;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rovearound.tripplanner.entities.Trip;
import com.rovearound.tripplanner.payloads.ApiResponse;
import com.rovearound.tripplanner.payloads.TripDto;
import com.rovearound.tripplanner.payloads.BudgetDto;
import com.rovearound.tripplanner.payloads.ExpenseDto;
import com.rovearound.tripplanner.payloads.ItineraryDto;
import com.rovearound.tripplanner.payloads.TravelerDto;
import com.rovearound.tripplanner.services.BudgetService;
import com.rovearound.tripplanner.services.ExpenseService;
import com.rovearound.tripplanner.services.ItineraryService;
import com.rovearound.tripplanner.services.TravelerService;
import com.rovearound.tripplanner.services.TripService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/trip/")
@CrossOrigin(origins = "http://localhost:4200")
public class TripController {
	@Autowired
	private TripService tripService;
	
	@Autowired
	private BudgetService budgetService;
	
	@Autowired
	private ExpenseService expenseService;
	
	@Autowired
	private ItineraryService itineraryService;
	
	@Autowired
	private TravelerService travelerService;
	
	@Autowired
	private ModelMapper modelMapper;

	@PostMapping("/add")
	public ResponseEntity<TripDto> createTrip(@Valid @RequestBody TripDto tripDto) {
		tripDto.setTripCode(this.generateTripCode());
		TripDto createdTripDto = this.tripService.createTrip(tripDto);
		this.createInitialBudgetForTrip(createdTripDto);
		this.createInitialExpenseForTrip(createdTripDto);
		this.createInitialItineraryForTrip(createdTripDto);
		this.createInitialTravelerForTrip(createdTripDto);
		return new ResponseEntity<>(createdTripDto, HttpStatus.CREATED);
	}

	@PutMapping("/{tripId}")
	public ResponseEntity<TripDto> updateTrip(@Valid @RequestBody TripDto tripDto, @PathVariable("tripId") Integer id) {
		TripDto updatedTrip = this.tripService.updateTrip(tripDto, id);
		return ResponseEntity.ok(updatedTrip);
	}

	@PostMapping("/{tripId}")
	public ResponseEntity<ApiResponse> deleteTrip(@PathVariable("tripId") Integer id) {
		this.tripService.deleteTrip(id);
		return new ResponseEntity<>(new ApiResponse("trip Deleted successfully", true), HttpStatus.OK);
	}

	@GetMapping("/all")
	public ResponseEntity<List<TripDto>> getAllTrips() {
		return ResponseEntity.ok(this.tripService.getAllTrips());
	}

	@GetMapping("/{tripId}")
	public ResponseEntity<TripDto> getTrip(@PathVariable Integer tripId) {
		ResponseEntity<TripDto> tripResponseEntity = ResponseEntity.ok(this.tripService.getTrip(tripId));
		TripDto trip = tripResponseEntity.getBody();
		List<TravelerDto> travelers = travelerService.getTravelersByTripId(tripId);
		return ResponseEntity.ok(this.tripService.getTrip(tripId));
	}
	
	private void createInitialBudgetForTrip(TripDto createdTripDto) {
		BudgetDto budgetDtoForCreatedTrip = new BudgetDto();
		
		budgetDtoForCreatedTrip.setAmount(0);
		budgetDtoForCreatedTrip.setStatus(true);
		budgetDtoForCreatedTrip.setTrip(this.dtoToTrip(createdTripDto));
		this.budgetService.createBudget(budgetDtoForCreatedTrip);
	}
	
	private void createInitialExpenseForTrip(TripDto createdTripDto) {
		ExpenseDto expenseDtoForCreatedTrip = new ExpenseDto();
		
		expenseDtoForCreatedTrip.setAmount(0);
		expenseDtoForCreatedTrip.setStatus(true);
		expenseDtoForCreatedTrip.setTrip(this.dtoToTrip(createdTripDto));
		expenseDtoForCreatedTrip.setUser(createdTripDto.getUser());
		expenseDtoForCreatedTrip.setPaidOn(null);
		expenseDtoForCreatedTrip.setSplitType("");
		expenseDtoForCreatedTrip.setCategory(null);
		expenseDtoForCreatedTrip.setCategoryDescription("");
		this.expenseService.createExpense(expenseDtoForCreatedTrip);
	}
	
	private void createInitialItineraryForTrip(TripDto createdTripDto) {
		ItineraryDto itineraryDtoForCreatedTrip = new ItineraryDto();
        itineraryDtoForCreatedTrip.setTrip(this.dtoToTrip(createdTripDto));
        itineraryDtoForCreatedTrip.setStatus(true);

        // Define the custom date-time formatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

        // Parse start date and end date using the custom formatter
        LocalDate startDate = LocalDate.parse(createdTripDto.getStartDate().toString(), formatter);
        LocalDate endDate = LocalDate.parse(createdTripDto.getEndDate().toString(), formatter);

        if (startDate.isBefore(endDate)) {
            long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);

            for (int i = 0; i <= daysDiff; i++) {
                LocalDate currentDate = startDate.plusDays(i);
                Date date = Date.from(currentDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
                itineraryDtoForCreatedTrip.setDate(date);
                this.itineraryService.createItinerary(itineraryDtoForCreatedTrip);
            }
        }

	}
	
	private void createInitialTravelerForTrip(TripDto createdTripDto) {
		TravelerDto travelerDtoForCreatedTrip = new TravelerDto();
		
		travelerDtoForCreatedTrip.setStatus(true);
		travelerDtoForCreatedTrip.setTrip(this.dtoToTrip(createdTripDto));
		travelerDtoForCreatedTrip.setUser(createdTripDto.getUser());
		this.travelerService.createTraveler(travelerDtoForCreatedTrip);
	}
	
	private Trip dtoToTrip(TripDto tripDto) {
		Trip trip = this.modelMapper.map(tripDto, Trip.class);
		return trip;
	}
	
	private String generateTripCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();

        Random random = new Random();

        for (int i = 0; i < 6; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }

        return sb.toString();
    }
}
