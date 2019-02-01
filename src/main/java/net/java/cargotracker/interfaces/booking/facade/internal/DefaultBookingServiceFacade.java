package net.java.cargotracker.interfaces.booking.facade.internal;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletionStage;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import net.java.cargotracker.application.BookingService;
import net.java.cargotracker.domain.model.cargo.Cargo;
import net.java.cargotracker.domain.model.cargo.CargoRepository;
import net.java.cargotracker.domain.model.cargo.Itinerary;
import net.java.cargotracker.domain.model.cargo.TrackingId;
import net.java.cargotracker.domain.model.location.Location;
import net.java.cargotracker.domain.model.location.LocationRepository;
import net.java.cargotracker.domain.model.location.UnLocode;
import net.java.cargotracker.domain.model.voyage.VoyageRepository;
import net.java.cargotracker.interfaces.booking.facade.BookingServiceFacade;
import net.java.cargotracker.interfaces.booking.facade.dto.CargoRoute;
import net.java.cargotracker.interfaces.booking.facade.dto.RouteCandidate;
import net.java.cargotracker.interfaces.booking.facade.internal.assembler.CargoRouteDtoAssembler;
import net.java.cargotracker.interfaces.booking.facade.internal.assembler.ItineraryCandidateDtoAssembler;
import net.java.cargotracker.interfaces.booking.facade.internal.assembler.LocationDtoAssembler;

@ApplicationScoped
public class DefaultBookingServiceFacade implements BookingServiceFacade,
        Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    private BookingService bookingService;
    @Inject
    private LocationRepository locationRepository;
    @Inject
    private CargoRepository cargoRepository;
    @Inject
    private VoyageRepository voyageRepository;

    @Override
    public List<net.java.cargotracker.interfaces.booking.facade.dto.Location> listShippingLocations() {
        List<Location> allLocations = locationRepository.findAll();
        LocationDtoAssembler assembler = new LocationDtoAssembler();
        return assembler.toDtoList(allLocations);
    }

    @Override
    public String bookNewCargo(String origin, String destination,
            Date arrivalDeadline) {
        TrackingId trackingId = bookingService.bookNewCargo(
                new UnLocode(origin), new UnLocode(destination),
                arrivalDeadline);
        return trackingId.getIdString();
    }

    @Override
    public CargoRoute loadCargoForRouting(String trackingId) {
        Cargo cargo = cargoRepository.find(new TrackingId(trackingId));
        CargoRouteDtoAssembler assembler = new CargoRouteDtoAssembler();
        return assembler.toDto(cargo);
    }

    @Override
    public void assignCargoToRoute(String trackingIdStr,
            RouteCandidate routeCandidateDTO) {
        Itinerary itinerary = new ItineraryCandidateDtoAssembler()
                .fromDTO(routeCandidateDTO, voyageRepository,
                        locationRepository);
        TrackingId trackingId = new TrackingId(trackingIdStr);

        bookingService.assignCargoToRoute(itinerary, trackingId);
    }

    @Override
    public void changeDestination(String trackingId, String destinationUnLocode) {
        bookingService.changeDestination(new TrackingId(trackingId),
                new UnLocode(destinationUnLocode));
    }

    @Override
    public List<CargoRoute> listAllCargos() {
        List<Cargo> cargos = cargoRepository.findAll();
        List<CargoRoute> routes = new ArrayList<>(cargos.size());

        CargoRouteDtoAssembler assembler = new CargoRouteDtoAssembler();

        for (Cargo cargo : cargos) {
            routes.add(assembler.toDto(cargo));
        }

        return routes;
    }

    @Override
    public Flowable<RouteCandidate> requestPossibleRoutesForCargo(String trackingId) {
        return bookingService
                .requestPossibleRoutesForCargo(new TrackingId(trackingId))
                .map(this::itineraryToDTO);
    }

    private RouteCandidate itineraryToDTO(Itinerary itinerary) throws Exception {
        ItineraryCandidateDtoAssembler dtoAssembler
                = new ItineraryCandidateDtoAssembler();
        return dtoAssembler.toDTO(itinerary);
    }
}
