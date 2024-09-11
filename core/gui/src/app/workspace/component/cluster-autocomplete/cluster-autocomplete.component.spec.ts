import { ComponentFixture, TestBed, waitForAsync } from "@angular/core/testing";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { ClusterAutoCompleteComponent } from "./cluster-autocomplete.component";
import { HttpClientTestingModule } from "@angular/common/http/testing";
import { NzModalService } from "ng-zorro-antd/modal";

describe("ClusterAutoCompleteComponent", () => {
  let component: ClusterAutoCompleteComponent;
  let fixture: ComponentFixture<ClusterAutoCompleteComponent>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ClusterAutoCompleteComponent],
      imports: [ReactiveFormsModule, HttpClientTestingModule],
      providers: [NzModalService],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ClusterAutoCompleteComponent);
    component = fixture.componentInstance;
    component.field = { props: {}, formControl: new FormControl() };
    fixture.detectChanges();
  });

  it("should create", () => {
    expect(component).toBeTruthy();
  });
});